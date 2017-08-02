# Zxing_Android3.3.0
[![](https://jitpack.io/v/github2136/Zxing_Android3.3.0.svg)](https://jitpack.io/#github2136/Zxing_Android3.3.0)  
本功能基于zxing3.3.0开发，除了zxing的core、android-core，使用少量代码实现二维码扫描功能，扫描界面为竖屏。  
```
Intent intent = new Intent(this, ZxingActivity.class);
//设置扫描框尺寸，宽高必须成对设置否则无效，如果同时设置DP和PX以PX为准
intent.putExtra(ZxingActivity.KEY_SCAN_WIDTH_DP,300);
intent.putExtra(ZxingActivity.KEY_SCAN_HEIGHT_DP,300);
intent.putExtra(ZxingActivity.KEY_SCAN_WIDTH_PX,300);
intent.putExtra(ZxingActivity.KEY_SCAN_HEIGHT_PX,300);
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