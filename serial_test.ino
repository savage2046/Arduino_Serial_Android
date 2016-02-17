// 设定SR04连接的Arduino引脚
const int TrigPin = 2;
const int EchoPin = 3;
String command = "";
void setup() {
  Serial.begin(115200);
  pinMode(TrigPin, OUTPUT);
  // 要检测引脚上输入的脉冲宽度，需要先设置为输入状态
  pinMode(EchoPin, INPUT);
}

void loop() {
  if (Serial.peek() != -1) {
    delay(10);//确保信息发送完成
    do {
      command += char(Serial.read());
    } while (Serial.peek() != -1);
  }
  if (command.length() > 0) {
    int idx = command.indexOf("read"); //read指令
    if (idx >= 0) {
      idx = command.indexOf("distance");
      if (idx >= 0) { // 检测脉冲宽度，并计算出距离
        getDistance();
      }
    } else {//确认存活
      Serial.print("online");
      Serial.println();
    }
    command = "";
  }
}
void getDistance() {
  // 产生一个10us的高脉冲去触发TrigPin
  digitalWrite(TrigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(TrigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(TrigPin, LOW);
  int distance = pulseIn(EchoPin, HIGH, 5801L)*10 / 58; //最大1000mm
  Serial.print("distance ");
  Serial.print(distance);
  Serial.print("mm");
  Serial.println();
}

