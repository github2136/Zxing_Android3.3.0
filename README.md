# Zxing_Android3.3.0
[![](https://jitpack.io/v/github2136/Zxing_Android3.3.0.svg)](https://jitpack.io/#github2136/Zxing_Android3.3.0)  
本功能基于zxing3.3.0开发，除了zxing的core、android-core，使用少量代码实现二维码扫描功能，扫描界面为竖屏，扫描框默认200dp*200dp。  
如需要做图片识别需要添加  
```
Intent intent = new Intent(this, ZxingActivity.class);
//扫描框宽度
intent.putExtra(ZxingActivity.ARG_SCAN_WIDTH_DP, 250);
//扫描框高度度
intent.putExtra(ZxingActivity.ARG_SCAN_HEIGHT_DP, 250);
//图片二维码扫描
intent.putExtra(ZxingActivity.ARG_SCAN_PIC, true);
//闪光灯
intent.putExtra(ZxingActivity.ARG_SCAN_FLASH, true);
//扫描框颜色
intent.putExtra(ZxingActivity.ARG_SCAN_COLOR, ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
//扫描线颜色
intent.putExtra(ZxingActivity.ARG_SCAN_LINE_COLOR, ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
//扫描线高度
intent.putExtra(ZxingActivity.ARG_SCAN_LINE_HEIGHT, 20f);
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
