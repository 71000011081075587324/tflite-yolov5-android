# tflite-yolov5
本模块参考教程: https://xugaoxiang.com/2022/09/06/yolov5-android-tflite
其中参考项目: https://github.com/xugaoxiang/yolov5_android_tflite

使用步骤：
1. 将 tflite_yolov5 模块文件夹拷入到项目根目录下
2. 在需要引入该 "tflite_yolov5" 的 module 的 build.gradle 的 dependencies 中 implementation(project(":tflite_yolov5"))
   代码调用：
      1. 调用 DetectorFactory.getDetector(Context context, int inputSize) 创建 YoloV5Classifier 对象
      2. 调用 yoloV5Classifier.recognizeImage(Bitmap bitmap), 将 bitmap 图像传入进行图像物体识别处理，处理完成后会返回识别物体信息
         返回的是 Recognition 类型数组对象, 每个 Recognition 中包含识别的物体名称以及物体的概率大小
      3. 最后当模型运行结束后，调用 Classifier.close() 方法关闭释放模型相关资源