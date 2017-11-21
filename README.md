# Zxing_Android3.3.0
[![](https://jitpack.io/v/github2136/Zxing_Android3.3.0.svg)](https://jitpack.io/#github2136/Zxing_Android3.3.0)  
本功能基于zxing3.3.0开发，除了zxing的core、android-core，使用少量代码实现二维码扫描功能，扫描界面为竖屏，扫描框默认200dp*200dp。
如需要做图片识别需要添加  
**compile 'com.github.github2136:Android-utils:ba2f38b4f2'**  
**compile 'com.google.code.gson:gson:2.7'**  
```
Intent intent = new Intent(this, ZxingActivity.class);
//设置扫描框尺寸，宽高必须成对设置否则无效，如果同时设置DP和PX以PX为准
intent.putExtra(ZxingActivity.AEG_SCAN_WIDTH_DP,300);
intent.putExtra(ZxingActivity.AEG_SCAN_HEIGHT_DP,300);
intent.putExtra(ZxingActivity.AEG_SCAN_WIDTH_PX,300);
intent.putExtra(ZxingActivity.AEG_SCAN_HEIGHT_PX,300);
intent.putExtra(ZxingActivity.AEG_SCAN_HEIGHT_SCALE, 0.0);//边框位置控制0.0-1.0
intent.putExtra(ZxingActivity.AEG_SCAN_COLOR, ResourcesCompat.getColor(getResources(),R.color.colorPrimary,null));//边框颜色设置，默认为Color.BLUE
startActivityForResult(intent, 1);

```
接受扫描结果
```
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
        Toast.makeText(this, data.getStringExtra(ZxingActivity.KEY_RESULT), Toast.LENGTH_SHORT).show();
    }
    super.onActivityResult(requestCode, resultCode, data);
}
```
