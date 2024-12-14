/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.tflite_yolov5.classifier;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import com.example.tflite_yolov5.utils.FileUtils;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;


/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * - https://github.com/tensorflow/models/tree/master/research/object_detection
 * where you can find the training code.
 * <p>
 * To use pretrained models in the API or convert to TF Lite models, please see docs for details:
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md#running-our-model-on-android
 */
public class YoloV5Classifier implements Classifier {

    /**
     * 最小置信度阈值
     */
    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f;

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param isQuantized   Boolean representing model is quantized or not
     */
    public static YoloV5Classifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final boolean isQuantized,
            /*final int[] output_width,
            final int[][] masks,
            final int[] anchors*/
            boolean isNNAPI,
            boolean isGPU)
            throws IOException {
        final YoloV5Classifier classifier = new YoloV5Classifier();

        // 读取 class 可识别类别文件
        InputStream labelsInput = assetManager.open(labelFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            classifier.labels.add(line);
        }
        br.close();

        // 生成 TensorFlow Lite 的解释器对象 Interpreter
        // 读取并解释已经转换为 TensorFlow Lite 模型格式的机器学习模型
        Interpreter.Options options = (new Interpreter.Options());
        options.setNumThreads(NUM_THREADS);
        // 默认为 false
        if (isNNAPI) {
            classifier.nnapiDelegate = null;
            // Initialize interpreter with NNAPI delegate for Android Pie or above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                classifier.nnapiDelegate = new NnApiDelegate();
                options.addDelegate(classifier.nnapiDelegate);
                options.setNumThreads(NUM_THREADS);
//                    options.setUseNNAPI(false);
//                    options.setAllowFp16PrecisionForFp32(true);
//                    options.setAllowBufferHandleOutput(true);
                options.setUseNNAPI(true);
            }
        }
        // 默认为 false
        if (isGPU) {
            GpuDelegate.Options gpu_options = new GpuDelegate.Options();
            gpu_options.setPrecisionLossAllowed(true); // It seems that the default is true
            gpu_options.setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED);
            classifier.gpuDelegate = new GpuDelegate(gpu_options);
            options.addDelegate(classifier.gpuDelegate);
        }
        // 读取加载 .tflite 模型文件
        classifier.tfliteModel = FileUtils.loadModelFile(assetManager, modelFilename);
        classifier.tfLite = new Interpreter(classifier.tfliteModel, options);


        // 默认为 false
        classifier.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        // 默认为 false
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            // 因为 float 类型占用 4 个字节，因此 numBytesPerChannel = 4，用于指定 imgData allocateDirect 时分配多大的内存
            numBytesPerChannel = 4; // Floating point
        }
        // 模型期望的 inputSize 输入尺寸参数, 不同模型的期望输入尺寸参数可能不同
        classifier.inputSize = classifier.tfLite.getInputTensor(0).shape()[1];
        // 下方计算式中 1 * classifier.inputSize * classifier.inputSize * 3 和 classifier.tfLite.getInputTensor(0).numElements() 获取值和计算方式相同
        classifier.imgData = ByteBuffer.allocateDirect(1 * classifier.inputSize * classifier.inputSize * 3 * numBytesPerChannel);
        // 设置 byteBuffer 的字节顺序为当前硬件平台的字节顺序(大 or 小端);
        // ByteOrder.nativeOrder() 用于获取当前硬件平台的字节顺序; byteBuffer.order() 是设置 byteBuffer 的字节顺序
        classifier.imgData.order(ByteOrder.nativeOrder());
        classifier.intValues = new int[classifier.inputSize * classifier.inputSize];

        /** 计算在经过模型运算后，输出的张量中包含了多少个预测框(bounding box)
            使用了 YOLOv3 目标检测模型中的计算公式来计算 output_box 的值。在 YOLOv3 中，模型将输入图像分成了多个网格单元（grid cell），
            每个网格单元包含了对应的预测框信息。output_box 表示了在这些网格单元中，总共包含了多少个预测框信息。
            具体计算方式为，首先计算出每个尺度上的网格数量，即 (classifier.INPUT_SIZE / 32)、(classifier.INPUT_SIZE / 16) 和 (classifier.INPUT_SIZE / 8)，
            然后将它们的平方相加并乘以 3，最终得到的结果就是 output_box
            output_box 的值和 classifier.tfLite.getOutputTensor(0).shape() 的第二个参数值相同
         */
        classifier.output_box = (int) ((Math.pow((classifier.inputSize / 32), 2) + Math.pow((classifier.inputSize / 16), 2) + Math.pow((classifier.inputSize / 8), 2)) * 3);
//        d.OUTPUT_WIDTH = output_width;
//        d.MASKS = masks;
//        d.ANCHORS = anchors;
        if (classifier.isModelQuantized){
            Tensor inpten = classifier.tfLite.getInputTensor(0);
            classifier.inp_scale = inpten.quantizationParams().getScale();
            classifier.inp_zero_point = inpten.quantizationParams().getZeroPoint();
            Tensor oupten = classifier.tfLite.getOutputTensor(0);
            classifier.oup_scale = oupten.quantizationParams().getScale();
            classifier.oup_zero_point = oupten.quantizationParams().getZeroPoint();
        }

        int[] shape = classifier.tfLite.getOutputTensor(0).shape();
        // 减去 5 是因为，shape[shape.length - 1] 的输出信息中还包含：左上角坐标，右下角坐标
        int numClass = shape[shape.length - 1] - 5;
        classifier.numClass = numClass; // 可识别物体种类个数
        //下方的 classifier.output_box * (numClass + 5) * numBytesPerChannel 和 classifier.tfLite.getOutputTensor(0).numElements() 值和计算方法相同
        classifier.outData = ByteBuffer.allocateDirect(classifier.output_box * (numClass + 5) * numBytesPerChannel);
        classifier.outData.order(ByteOrder.nativeOrder());
        return classifier;
    }


    @Override
    public void close() {
        tfLite.close(); // 关闭 Interpreter 并释放与之相关联的资源
        tfLite = null;
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        if (nnapiDelegate != null) {
            nnapiDelegate.close();
            nnapiDelegate = null;
        }
        tfliteModel = null;
    }

    // 设置线程数来控制模型推理过程中的并行度
    public void setNumThreads(int num_threads) {
        if (tfLite != null) tfLite.setNumThreads(num_threads);
    }

    @Override
    public float getObjThresh() {
        return MINIMUM_CONFIDENCE_TF_OD_API;
    }

    // Float model
    private final float IMAGE_MEAN = 0;

    // 用于归一化，将像素值转换为 0~1 之间的值 【因为 RGB 每个通道的取值范围都在 0～255 之间】
    // 在深度学习中，对图像数据进行预处理时，需要将像素值进行归一化，使其变为均值为 0、方差为 1 的标准分布，这是为了让数据变得更容易处理
    private final float IMAGE_STD = 255.0f;

    //config yolo
    // 模型期望的 inputSize 输入尺寸参数, 不同模型的期望输入尺寸参数可能不同
    private int inputSize = -1;

//    private int[] OUTPUT_WIDTH;
//    private int[][] MASKS;
//    private int[] ANCHORS;
    private  int output_box;

    private static final float[] XYSCALE = new float[]{1.2f, 1.1f, 1.05f};

    private static final int NUM_BOXES_PER_BLOCK = 3;

    // Number of threads in the java app
    private static int NUM_THREADS = 1;

    // 默认为 false
    private boolean isModelQuantized;

    /** holds a gpu delegate */
    GpuDelegate gpuDelegate = null;
    /** holds an nnapi delegate */
    NnApiDelegate nnapiDelegate = null;

    /** The loaded TensorFlow Lite model. */
    private MappedByteBuffer tfliteModel;

    /** Options for configuring the Interpreter. */
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    // Config values.

    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;

    private ByteBuffer imgData;
    private ByteBuffer outData;

    private Interpreter tfLite;
    private float inp_scale;
    private int inp_zero_point;
    private float oup_scale;
    private int oup_zero_point;
    private int numClass;
    private YoloV5Classifier() {
    }

    //non maximum suppression
    protected ArrayList<Recognition> nms(ArrayList<Recognition> list) {
        ArrayList<Recognition> nmsList = new ArrayList<Recognition>();

        for (int k = 0; k < labels.size(); k++) {
            //1.find max confidence per class
            PriorityQueue<Recognition> pq =
                    new PriorityQueue<Recognition>(
                            50,
                            new Comparator<Recognition>() {
                                @Override
                                public int compare(final Recognition lhs, final Recognition rhs) {
                                    // Intentionally reversed to put high confidence at the head of the queue.
                                    return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                                }
                            });

            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).getDetectedClass() == k) {
                    pq.add(list.get(i));
                }
            }

            //2.do non maximum suppression
            while (pq.size() > 0) {
                //insert detection with max confidence
                Recognition[] a = new Recognition[pq.size()];
                Recognition[] detections = pq.toArray(a);
                Recognition max = detections[0];
                nmsList.add(max);
                pq.clear();

                for (int j = 1; j < detections.length; j++) {
                    Recognition detection = detections[j];
                    RectF b = detection.getLocation();
                    if (box_iou(max.getLocation(), b) < mNmsThresh) {
                        pq.add(detection);
                    }
                }
            }
        }
        return nmsList;
    }

    protected float mNmsThresh = 0.6f;

    protected float box_iou(RectF a, RectF b) {
        return box_intersection(a, b) / box_union(a, b);
    }

    protected float box_intersection(RectF a, RectF b) {
        float w = overlap((a.left + a.right) / 2, a.right - a.left,
                (b.left + b.right) / 2, b.right - b.left);
        float h = overlap((a.top + a.bottom) / 2, a.bottom - a.top,
                (b.top + b.bottom) / 2, b.bottom - b.top);
        if (w < 0 || h < 0) return 0;
        float area = w * h;
        return area;
    }

    protected float box_union(RectF a, RectF b) {
        float i = box_intersection(a, b);
        float u = (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i;
        return u;
    }

    protected float overlap(float x1, float w1, float x2, float w2) {
        float l1 = x1 - w1 / 2;
        float l2 = x2 - w2 / 2;
        float left = l1 > l2 ? l1 : l2;
        float r1 = x1 + w1 / 2;
        float r2 = x2 + w2 / 2;
        float right = r1 < r2 ? r1 : r2;
        return right - left;
    }

    protected static final int BATCH_SIZE = 1;
    protected static final int PIXEL_SIZE = 3;

    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    protected void convertBitmapToByteBufferInImgData(Bitmap bitmap) {
        // 从一个 Bitmap 对象中获取所有像素的颜色值，并将这些颜色值存储在一个名为 intValues 的整型数组中
        // intValues 数组大小等于 bitmap 的 width*height
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();   // 重制 position 为 0
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point));
                    imgData.put((byte) ((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point));
                    imgData.put((byte) (((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point));
                } else { // Float model
                    /** 将 bitmap 转换为模型输入张量的浮点数数据
                     *  bitmap 的每个像素值是一个 4 字节的整数，它描述了该像素的 Alpha，Red，Green 和 Blue 通道
                     * 与 0xFF & 即可得到当前最低 8 位的值
                     */
                    // (pixelValue >> 16) & 0xFF 得到了红色通道值，然后除以 IMAGE_STD 进行归一化到 0～1 之间
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    // (pixelValue >> 8) & 0xFF 得到了绿色通道值，然后除以 IMAGE_STD 进行归一化到 0～1 之间
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    // pixelValue & 0xFF 得到了蓝色通道值，然后除以 IMAGE_STD 进行归一化到 0～1 之间
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
    }

    // 图像物体检测
    public ArrayList<Recognition> recognizeImage(Bitmap bitmap) {
        // 将 bitmap 转换为模型输入张量存入到 imgData 变量中
        convertBitmapToByteBufferInImgData(bitmap);
        Object[] inputArray = {imgData};

        // 将 outData 放到 Map 类型中
        Map<Integer, Object> outputMap = new HashMap<>();
        outData.rewind();   // 重制 position 为 0
        outputMap.put(0, outData);

        // 执行模型推理, 根据输入数据 inputArray 执行模型的推理，然后将输出结果写入到输出张量 outputMap 中
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        ByteBuffer outputByteBuffer = (ByteBuffer) outputMap.get(0);
        outputByteBuffer.rewind();    // 重制 position 为 0

        ArrayList<Recognition> detections = new ArrayList<Recognition>();

        // 1 output_box numClass + 5 分别对应于 tfLite.getOutputTensor(0).shape() 数组中的三个数
        float[][][] out = new float[1][output_box][numClass + 5];
        for (int i = 0; i < output_box; ++i) {
            for (int j = 0; j < numClass + 5; ++j) {
                if (isModelQuantized){
                    out[0][i][j] = oup_scale * (((int) outputByteBuffer.get() & 0xFF) - oup_zero_point);
                } else {
                    // 从当前位置读取 4 个字节，并将这 4 个字节按照当前字节顺序转换成一个 float 类型的值返回，然后将 outputByteBuffer 的 position 属性增加 4
                    out[0][i][j] = outputByteBuffer.getFloat();
                }
            }
            // Denormalize xywh
            for (int j = 0; j < 4; ++j) {
                // 分别是：左上角的 x 坐标，左上角的 y 坐标，边界框 width 值，边界框 Height 值
                out[0][i][j] *= inputSize;
            }
        }
        for (int i = 0; i < output_box; ++i){
            final int offset = 0;
            final float confidence = out[0][i][4];  // 置信度, 表示该边界框含有物体的概率
            int detectedClass = -1;
            float maxClass = 0;

            final float[] classesProbability = new float[labels.size()];
            // 所有可识别种类的可能性
            for (int c = 0; c < labels.size(); ++c) {
                classesProbability[c] = out[0][i][5 + c];
            }

            // 找到所有种类的可能性值最大的一个物体类别
            for (int c = 0; c < labels.size(); ++c) {
                if (classesProbability[c] > maxClass) {
                    detectedClass = c;
                    maxClass = classesProbability[c];
                }
            }

            /**
             *   判断是否大于最小置信度阈值：
             *   如果小于的话，则可以忽略或视为噪音
             *   如果大于的话，计算出物体所在检测框位置，并将可能性最大的物体名称以及可能性大小记录下来
              */
            final float confidenceInClass = maxClass * confidence;
            if (confidenceInClass > getObjThresh()) {
                final float xPos = out[0][i][0];
                final float yPos = out[0][i][1];
                final float w = out[0][i][2];
                final float h = out[0][i][3];

                final RectF rect =
                        new RectF(
                                Math.max(0, xPos - w / 2),
                                Math.max(0, yPos - h / 2),
                                Math.min(bitmap.getWidth() - 1, xPos + w / 2),
                                Math.min(bitmap.getHeight() - 1, yPos + h / 2));
                detections.add(new Recognition("" + offset, labels.get(detectedClass),
                        confidenceInClass, rect, detectedClass));
            }
        }

        final ArrayList<Recognition> recognitions = nms(detections);
//        Log.d("fzc", "recognitions.size = " + recognitions.size());
        return recognitions;
    }

    public int getInputSize() {
        return inputSize;
    }
}
