# Arduino_Serial_Android
rewrite hoho example, easy to use
安卓连接arduino
基于HOHO的库
默认的波特率为：115200
重写操作流程,对驱动只做细微修改，更方便使用
改进获得权限方式，同时用静态（AndroidManifest.xml）和动态（Broadcast）用两种方式,保证无论何时拔插都能正确获得权限
