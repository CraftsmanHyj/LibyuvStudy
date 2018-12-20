# LibyuvStudy
腾讯实视音视频在被叫端获取的I420数据转Bitmap的全过程；

代码中I420直接转Argb代码，转码之后存在颜色不对的问题，**暂时未解决**有解决办法的可以fork项目，提交代码；

这里通过I420先转成NV21数据，然后将NV21转argb，再转成bitmap的方式来间接的达到效果。

[博文解释说明](https://blog.csdn.net/uu00soldier/article/details/85141675)