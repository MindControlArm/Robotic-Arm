
#define MAX_DC_SPEED 100
#define MAX_DC_SPEED_R 50

//Actuator1
#define Act1In1Pin 6  //Positive Pin
#define Act1In2Pin 5 //Negative Pin 
#define Act1EnbPin 7  //PWM Speed PIN
#define Act1PotPin A0  //Potentiometer PIN
#define Act1length 150  //Potentiometer PIN
//Actuator2
#define Act2In1Pin 4  //Positive Pin
#define Act2In2Pin 3 //Negative Pin 
#define Act2EnbPin 2  //PWM Speed PIN
#define Act2PotPin A1  //Potentiometer PIN
#define Act2length 100  //Potentiometer PIN
//100mm 0.3-9.6K (33-984) 0-100
//150mm 0.3-8.6K (28-678) 0-150

//dimentions for Linear movment calculations
double la1 =10;// fixed
double la2 =10;// fixed
double la3 = 12;// fixed
double l0 = 10;//fixed
double l1 =30;//fixed
double l2 =30;//fixed

//      lb1
//---------------------
//-             |    -
//-             |  -  la1
//-------------- -
//     l0          teta1

//   |--teta2
//   |   -
//   |    -  la3
//la2|    / -
//   |  /lb2  -
//   |/
//   |

//Servos
#include <Servo.h> 
Servo servoGriper; 
Servo servoPan; 
Servo servoTilt; 
#define servoGriperPin 9  //Positive Pin
#define servoPanPin 1 //Positive Pin
#define servoTiltPin 1  //Positive Pin

//DC MOTORS
#include <Encoder.h>
#define FORWARD         1                       // direction of rotation
#define BACKWARD        2                       // direction of rotation

//DC MOTOR 1
#define   M1InA1            22                      // INA motor pin
#define   M1InB1            24                      // INB motor pin 
#define   M1PWM1            8                       // PWM motor pin
#define   M1encodPinA1      18                       // encoder A pin
#define   M1encodPinB1      19                       // encoder B pin
Encoder   M1Enc(M1encodPinA1, M1encodPinB1);
long      M1oldPosition  = -999;

byte M1PWMOutput;
long M1Error[10];
long M1Accumulator;
long M1PID;
int M1PTerm;
int M1ITerm;
int M1DTerm;
byte M1Divider;
byte M1Mdirection = FORWARD;

//DC MOTOR 2
#define   M2InA1            26                      // INA motor pin
#define   M2InB1            28                      // INB motor pin 
#define   M2PWM1            11                      // PWM motor pin
#define   M2encodPinA1      16                       // encoder A pin
#define   M2encodPinB1      17                       // encoder B pin
Encoder   M2Enc(M2encodPinA1, M2encodPinB1);
long      M2oldPosition  = -999;

byte M2PWMOutput;
long M2Error[10];
long M2Accumulator;
long M2PID;
int M2PTerm;
int M2ITerm;
int M2DTerm;
byte M2Divider;
byte M2Mdirection = FORWARD;

//DC MOTOR 3
#define   M3InA1            22                      // INA motor pin
#define   M3InB1            24                      // INB motor pin 
#define   M3PWM1            8                       // PWM motor pin
#define   M3encodPinA1      18                       // encoder A pin
#define   M3encodPinB1      19                       // encoder B pin
Encoder   M3Enc(M3encodPinA1, M3encodPinB1);
long      M3oldPosition  = -999;

byte M3PWMOutput;
long M3Error[10];
long M3Accumulator;
long M3PID;
int M3PTerm;
int M3ITerm;
int M3DTerm;
byte M3Divider;
byte M3Mdirection = FORWARD;

//Done

char charRead; //last char from serial

//------------------------------------------
#include <Wire.h>
#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include <Servo.h> 

AndroidAccessory acc("Mind Control Project TOM 2014",
"MCP",
"MCP Mega ADK",
"0.1",
"http://www.tomisrael.org",
"0000000087654321");

// These should match the command definitions in the commands section 
// of the .json file
#define WHICHDEVICE 0
#define ARMROTATE 1
#define GRIPPERUPDOWN 2
#define EXTAND 3
#define GRIPPERROTATE 4
#define ARM 5
#define GRIPPER 6
#define GRIPPERLEFTRIGHTENDPOINT 7o
#define GRIPPERUPDOWNENDPOINT 8
#define GRIPPERROTATEENDPOINT 9
#define GRIPPERLEFTRIGHT 10

#define STOP 10

void gripperMoveUp();
void gripperMoveDown();
void gripperMoveRight();
void gripperMoveLeft();
void armRotateLeft();
void armRotateRight();
void armUp();
void armDown();
void armRetract();
void armExtand();


void setup();
void loop();

#define BAUDRATE 115200
void setup()
{
  Serial.begin(BAUDRATE);
  Serial.print("\r\nStart\r\n");
  motorSetup();
  Serial.println("motor setup completed");

  acc.powerOn();
  Serial.println("initial setup completed");
}

void loop()
{
  byte err;
  byte idle;
  static byte count = 0;
  byte msg[3];
  //  long touchcount;

  if (acc.isConnected()) {
    int len = acc.read(msg, sizeof(msg), 1);
    int i;
    byte b;
    byte b1 = 0;
    uint16_t val;
    int x, y;
    char c0;

    if (len > 0) {
      String printthis = "\r\nReceived command ";

      Serial.print(printthis + msg[0] + "," + msg[1] + "," + msg[2] + "\r\n");

      // assumes only one command per packet
      if (msg[0] == WHICHDEVICE) {
        msg[0] = WHICHDEVICE;
        msg[1] = 1; // This needs to be changed later
        msg[2] = 0; // Currently unused
        String printthis2 = "\r\nSent message: ";
        Serial.println(printthis2 + msg[0] + msg[1] + msg[2]);
        acc.write(msg, 3);
      } 
      switch (msg[0]){

      case GRIPPERUPDOWN:
        Serial.println("GRIPPER UP-DOWN");
        if(msg[1] == 1) //UP
        {
          Serial.println("GRIPPER UP");
          gripperMoveUp();
        }
        else if (msg[1] == 0) //DOWN
        {
          Serial.println("GRIPPER DOWN");
          gripperMoveDown();
        }
        break;

      case GRIPPERLEFTRIGHT:
        Serial.println("GRIPPER RIGHT-LEFT");
        if(msg[1] == 1) //RIGHT
        {
          Serial.println("GRIPPER RIGHT");
          gripperMoveRight();
        }

        else if (msg[1] == 0) //LEFT
        {
          Serial.println("GRIPPER LEFT");
          gripperMoveLeft();
        }


        break;
      case ARMROTATE:
        Serial.println("ARMROTATE....");
        if(msg[1] == 1) //RIGHT
        {
          Serial.println("ARM ROTATE RIGHT");
          armRotateRight();
        }

        else if (msg[1] == 0) //LEFT
        {
          Serial.println("ARM ROTATE LEFT");
          armRotateLeft();

        }


        break;

      case GRIPPER:
        Serial.println("GRIPPER....");
        if(msg[1] == 0)
        {
          Serial.println("GRIPPER OPEN");
          gripperOpen();
        }
        else if (msg[1] == 1)
        {
          Serial.println("GRIPPER CLOSE");
          gripperClose();
        }
        break;

      case ARM:
        Serial.println("ARM....");
        if(msg[1] == 1)
        {
          Serial.println("ARM UP");
          armUp();
        }
        else if (msg[1] == 0)
        {
          Serial.println("ARM DOWN");
          armDown();
        }
        break;


     case EXTAND:
        Serial.println("EXTAND-RETRACT....");
        if(msg[1] == 1)
        {
          Serial.println("EXTAND");
          armExtand();
        }
        else if (msg[1] == 0)
        {
          Serial.println("RETRACT");
          armRetract();
        }
        break;

        //JASON jason-----------!!!!!!!!!!!!!!!!!!----JASON jason!
        //The only missing commands are 
        //the "rotate gripper up  and rotate gripper down.
        //I couldn't truly understand what it does.(or wouldn't :P)
        // can you handle the stubs?"

      }
    }
  }
  msg[0] = 0x1;
} 
//-----------------------------------------------------------------------------------------
/**
 * Move the gripper upwards by a constant ROTATION_DEGREE 
 */
void gripperMoveUp()
{
      Serial.println("TBD");
      motor2Forward(50);
      delay(100);
      motor2Brake();

}

/**
 * Move the gripper downwards by a constant ROTATION_DEGREE
 */
void gripperMoveDown()
{
      Serial.println("TBD");
      motor2Backward(50);
      delay(100);
      motor2Brake();
}

//      
//            moveAct(Act1In1Pin,Act1In2Pin,Act1EnbPin,true,1000);

//      moveAct(Act2In1Pin,Act2In2Pin,Act2EnbPin,true,1000);
//grippper
//       for(int i=0;i<300;i++){
//        movePID3(300);
//      }
//      
//      
/**
 * Move the gripper downwards by a constant ROTATION_DEGREE
 */
void gripperOpen()
{
  servoGriper.write(180);
}

/**
 * Move the gripper downwards by a constant ROTATION_DEGREE
 */
void gripperClose()
{
  servoGriper.write(0);
}

/**
 * Move the gripper base to the right
 */
void gripperMoveRight()
{
      Serial.println("rotate gripper right");
      for(int i=0;i<100;i++){
        movePID3(0);
      }
}

/**
 * Move the gripper base to the left
 */
void gripperMoveLeft()
{
  Serial.println("rotate gripper left");
  for(int i=0;i<100;i++){
    movePID3(2000);
  }
}

/** 
 * Rotate arm's base to the left by a constant ROTATION_DEGREE
 */
void armRotateLeft()
{

}

/**
 * Rotate arm's base to the right by a constant ROTATION_DEGREE
 */
void armRotateRight()
{

}


///**
// * Rotate the gripper to the right by a constant ROTATION_DEGREE
// */
//void gripperRotateRight()
//{
//}
//
//
///**
// * Rotate the gripper to the left by a constant ROTATION_DEGREE
// */
//void gripperRotateLeft()
//{
//
//
///**
// * Move the arm up by a constant ROTATION_DEGREE
// */
void armUp(){
   Serial.println("Act2 UP");
      moveAct(Act2In1Pin,Act2In2Pin,Act2EnbPin,true,1000);

 

}


/**
 * Move the arm down by a constant ROTATION_DEGREE
 */
void armDown(){
     Serial.println("Act2 Down");
      moveAct(Act2In1Pin,Act2In2Pin,Act2EnbPin,false,1000);
}
void armExtand(){
   Serial.println("Act2 UP");
      moveAct(Act1In1Pin,Act1In2Pin,Act1EnbPin,true,1000);
}

/**
 * Move the arm down by a constant ROTATION_DEGREE
 */
void armRetract(){
     Serial.println("Act2 Down");
      moveAct(Act1In1Pin,Act1In2Pin,Act1EnbPin,false,1000);
}



//-------------------------------------------------------------------------------------------------------

void motorSetup(){

  //ACT2
  pinMode(Act1In1Pin, OUTPUT);
  pinMode(Act1In2Pin, OUTPUT);
  pinMode(Act1EnbPin, OUTPUT);
  //ACT1
  pinMode(Act2In1Pin, OUTPUT);
  pinMode(Act2In2Pin, OUTPUT);
  pinMode(Act2EnbPin, OUTPUT);
  //M1
  pinMode(M1InA1, OUTPUT);
  pinMode(M1InB1, OUTPUT);
  pinMode(M1PWM1, OUTPUT);
  //M2
  pinMode(M2InA1, OUTPUT);
  pinMode(M2InB1, OUTPUT);
  pinMode(M2PWM1, OUTPUT);

  //M3
  pinMode(M3InA1, OUTPUT);
  pinMode(M3InB1, OUTPUT);
  pinMode(M3PWM1, OUTPUT);
  //Servos
  servoGriper.attach(servoGriperPin);
  servoPan.attach(servoPanPin);
  servoTilt.attach(servoTiltPin);
  //Done  
//        Serial.println("rotate gripper home");
//      for(int i=0;i<100;i++){
//        movePID3(1000);
//      }
  
  
  
  Serial.println("initial setup completed");  
}

//void loop(){
//  checkSerial();
//}

void checkSerial(){
  if (Serial.available()) {
    charRead = Serial.read();
    switch (charRead){
    case '1':
      Serial.println("Act1 UP");
      moveAct(Act1In1Pin,Act1In2Pin,Act1EnbPin,true,1000);
      break;
    case 'q':
      Serial.println("Act1 Down");
      moveAct(Act1In1Pin,Act1In2Pin,Act1EnbPin,false,1000);

      break;
    case 'a':
      Serial.print("Act1 Potentiometer");
      Serial.println(readAct(Act1PotPin,Act1length));
      break;
    case '2':
      Serial.println("Act2 UP");
      moveAct(Act2In1Pin,Act2In2Pin,Act2EnbPin,true,1000);
      break;
    case 'w':
      Serial.println("Act2 Down");
      moveAct(Act2In1Pin,Act2In2Pin,Act2EnbPin,false,1000);
      break;
    case 's':
      Serial.print("Act2 Potentiometer: ");
      Serial.println(readAct(Act2PotPin,Act2length));
      break;
    case 't':
      Serial.print("teta1 calculation in radians: ");
      Serial.println(getArmBaseAngle());
      break;      
    case 'y':
      Serial.print("teta2 calculation in radians: ");
      Serial.println(getArmElbowAngle());
      break;        
    case '3':
      Serial.println("rotate gripper");
      for(int i=0;i<300;i++){
        movePID3(300);
      }
      break;
    case 'e':
      Serial.println("TBD");
      Serial.println("rotate gripper");
      for(int i=0;i<500;i++){
        movePID3(0);
      }
      break;
    case 'd':
      Serial.print("TBD");
      Serial.println("rotate gripper");
      for(int i=0;i<500;i++){
        movePID3(2000);
      }
      break;
    case '4':
      Serial.println("gripper close");
      servoGriper.write(0);

      break;
    case 'r':
      Serial.println("gripper open");
      servoGriper.write(180);

      break;
    case 'f':
      Serial.println("gripper 0");
      servoGriper.write(0);
      delay(1000);
      Serial.println("gripper 45");
      servoGriper.write(45);
      delay(1000);
      Serial.println("gripper 90");
      servoGriper.write(90);
      delay(1000);
      Serial.println("gripper 135");
      servoGriper.write(135);
      break;
    case '9':
      Serial.println("rotate arm to 0");

      break;
    case 'o':
      Serial.println("TBD");
      motor2Forward(50);
      delay(300);
      motor2Brake();
      break;
    case 'l':
      Serial.println("TBD");
      motor2Backward(50);
      delay(300);
      motor2Brake();
      break;
    }
  }
}

//---------Actuators CONTROL----------

void moveAct(int pinP,int pinN, int pinE, boolean moveUp,int time){
  if (moveUp){
    digitalWrite (pinP,LOW);
    digitalWrite (pinN,HIGH);
  }
  else {
    digitalWrite (pinP,HIGH);
    digitalWrite (pinN,LOW);
  }
  digitalWrite(pinE,HIGH);
  delay(time);
  digitalWrite(pinE,LOW);

}


int readAct(int pin,int maxlength){
  int result = analogRead(pin);
  Serial.println(result);

  if (maxlength == 150){
    result = map(result, 28, 878, 0, 150);
  } 
  else{
    result = map(result, 33, 984, 0, 100);

  }
  return result;
}

//100mm 0.3-9.6K (33-984) 0-100
//150mm 0.3-8.6K (28-678) 0-150
double getArmBaseAngle(){
  double lb1 = 12;//
  double teta1 = 0;//
  teta1 = asin((1/la1)*(lb1-l0));
  return teta1; //in radians
  //      lb1
  //---------------------
  //-             |    -
  //-             |  -  la1
  //-------------- -
  //     l0          teta1
}

double getArmElbowAngle(){
  double lb2 =10;// changable
  double teta2 = 0;//
  teta2 = acos(((la2*la2)+(la3*la3)-(lb2*lb2))/(2*la2*la3));
  return teta2; //in radians
  //   |--teta2
  //   |   -
  //   |    -  la3
  //la2|    / -
  //   |  /lb2  -
  //   |/
  //   |
}
//---------ACTUATOR VECTOR CONTROL----------

double  moveXY(double x, double y){
  double a = 0;// fixed
  double b = 0;// fixed
  double c = 0;// changable
  double d = 0;//
  double newlb1 = 0;//
  double newlb2 = 0;//
  double teta1 = getArmBaseAngle();//
  double teta2 = getArmElbowAngle();//
  double newTeta1;
  double newTeta2;
  a = l1 *cos(teta1)+l2*cos(teta1+PI-teta2);
  b = -l2*cos(teta1+PI-teta2);
  c = -l1 *sin(teta1)+l2*sin(teta1+PI-teta2);
  d = -l2 *sin(teta1+PI-teta2);

  newTeta1 = teta1+((x*d-b*y)/((a*d)-(b*c)));
  newTeta2 = teta2+((-c*x+a*y)/((a*d)-(b*c)));

  newlb1 = l0+la1*sin(newTeta1);
  newlb2 = sqrt(sq(la2)+sq(la3)-2*la2*la3*cos(newTeta2));
  //move actuators to newlb1/2
}

//---------DC MOTOR 1 CONTROL----------
void movePID1(long location){
  Serial.println(updatePos1());
  GetError1(location);       // Get position error
  CalculatePID1();   // Calculate the PID output from the error
  moveMotor1 (M1Mdirection,M1PWMOutput,0);

}
long updatePos1 (){
  long newPosition1 = M1Enc.read();
  if (newPosition1 != M1oldPosition) {
    M1oldPosition = newPosition1;
  }
  return newPosition1;
}
void moveMotor1(int Mdirection, int PWM_val, long tick)  {
  //  countInit = count;    // abs(count)
  //  tickNumber = tick;
  if(Mdirection==FORWARD)          motor1Forward(PWM_val);
  else if(Mdirection==BACKWARD)    motor1Backward(PWM_val);
}

void motor1Forward(int PWM_val)  {
  analogWrite(M1PWM1, PWM_val);
  digitalWrite(M1InA1, LOW);
  digitalWrite(M1InB1, HIGH);
  //  run = true;
}

void motor1Backward(int PWM_val)  {
  analogWrite(M1PWM1, PWM_val);
  digitalWrite(M1InA1, HIGH);
  digitalWrite(M1InB1, LOW);
  //  run = true;
}

void motor1Brake()  {
  analogWrite(M1PWM1, 0);
  digitalWrite(M1InA1, HIGH);
  digitalWrite(M1InB1, HIGH);
  //  run = false;
}

void GetError1(long DesiredPosition)
{
  byte i = 0;
  // read analogs
  long ActualPosition = updatePos1();  
  // comment out to speed up PID loop
  //  Serial.print("ActPos= ");
  //  Serial.println(ActualPosition,DEC);

  //  long DesiredPosition = 1000;
  // comment out to speed up PID loop
  //  Serial.print("DesPos= ");
  //  Serial.println(DesiredPosition,DEC);

  // shift error values
  for(i=0;i<10;i++)
    M1Error[i+1] = M1Error[i];
  // load new error into top array spot  
  M1Error[0] = (long)DesiredPosition-(long)ActualPosition;
  // comment out to speed up PID loop
  //  Serial.print("Error= ");
  //  Serial.println(Error[0],DEC);

}

/* CalculatePID():
 Error[0] is used for latest error, Error[9] with the DTERM
 */
void CalculatePID1(void)
{
  // Set constants here
  M1PTerm = 2000;
  M1ITerm = 25;
  M1DTerm = 0;
  M1Divider = 10;

  // Calculate the PID  
  M1PID = M1Error[0]*M1PTerm;     // start with proportional gain
  M1Accumulator += M1Error[0];  // accumulator is sum of errors
  M1PID += M1ITerm*M1Accumulator; // add integral gain and error accumulation
  M1PID += M1DTerm*(M1Error[0]-M1Error[9]); // differential gain comes next
  M1PID = M1PID>>M1Divider; // scale PID down with divider

    // comment out to speed up PID loop  
  //Serial.print("PID= ");
  //  Serial.println(PID,DEC);

  // limit the PID to the resolution we have for the PWM variable

  if(M1PID>=MAX_DC_SPEED)
    M1PID = MAX_DC_SPEED;
  if(M1PID<=-MAX_DC_SPEED)
    M1PID = -MAX_DC_SPEED;

  //PWM output should be between 1 and 254 so we add to the PID    
  M1PWMOutput = abs(M1PID);
  if (M1PID<0) M1Mdirection = BACKWARD;
  else M1Mdirection = FORWARD;

  // comment out to speed up PID loop
  //  Serial.print("PWMOutput= ");
  //  Serial.println(PWMOutput,DEC);

}

//---------DC MOTOR 2 CONTROL----------
void movePID2(long location){
  Serial.println(updatePos2());
  GetError2(location);       // Get position error
  CalculatePID2();   // Calculate the PID output from the error
  moveMotor2 (M2Mdirection,M2PWMOutput,0);

}
long updatePos2 (){
  long newPosition2 = M2Enc.read();
  if (newPosition2 != M2oldPosition) {
    M2oldPosition = newPosition2;
  }
  return newPosition2;
}
void moveMotor2(int Mdirection, int PWM_val, long tick)  {
  //  countInit = count;    // abs(count)
  //  tickNumber = tick;
  if(Mdirection==FORWARD)          motor2Forward(PWM_val);
  else if(Mdirection==BACKWARD)    motor2Backward(PWM_val);
}

void motor2Forward(int PWM_val)  {
  analogWrite(M2PWM1, PWM_val);
  digitalWrite(M2InA1, LOW);
  digitalWrite(M2InB1, HIGH);
  //  run = true;
}

void motor2Backward(int PWM_val)  {
  analogWrite(M2PWM1, PWM_val);
  digitalWrite(M2InA1, HIGH);
  digitalWrite(M2InB1, LOW);
  //  run = true;
}

void motor2Brake()  {
  analogWrite(M2PWM1, 0);
  digitalWrite(M2InA1, HIGH);
  digitalWrite(M2InB1, HIGH);
  //  run = false;
}

void GetError2(long DesiredPosition)
{
  byte i = 0;
  // read analogs
  long ActualPosition = updatePos2();  
  // comment out to speed up PID loop
  //  Serial.print("ActPos= ");
  //  Serial.println(ActualPosition,DEC);

  //  long DesiredPosition = 1000;
  // comment out to speed up PID loop
  //  Serial.print("DesPos= ");
  //  Serial.println(DesiredPosition,DEC);

  // shift error values
  for(i=0;i<10;i++)
    M2Error[i+1] = M2Error[i];
  // load new error into top array spot  
  M2Error[0] = (long)DesiredPosition-(long)ActualPosition;
  // comment out to speed up PID loop
  //  Serial.print("Error= ");
  //  Serial.println(Error[0],DEC);

}

/* CalculatePID():
 Error[0] is used for latest error, Error[9] with the DTERM
 */
void CalculatePID2(void)
{
  // Set constants here

  M2PTerm = 100;
  M2ITerm = 25;
  M2DTerm = 10;
  M2Divider = 10;

  // Calculate the PID  
  M2PID = M2Error[0]*M2PTerm;     // start with proportional gain
  M2Accumulator += M2Error[0];  // accumulator is sum of errors
  M2PID += M2ITerm*M2Accumulator; // add integral gain and error accumulation
  M2PID += M2DTerm*(M2Error[0]-M2Error[9]); // differential gain comes next
  M2PID = M2PID>>M2Divider; // scale PID down with divider

    // comment out to speed up PID loop  
  //Serial.print("PID= ");
  //  Serial.println(PID,DEC);

  // limit the PID to the resolution we have for the PWM variable

  if(M2PID>=MAX_DC_SPEED_R)
    M2PID = MAX_DC_SPEED_R;
  if(M2PID<=-MAX_DC_SPEED_R)
    M2PID = -MAX_DC_SPEED_R;

  //PWM output should be between 1 and 254 so we add to the PID    
  M2PWMOutput = abs(M2PID);
  if (M2PID<0) M2Mdirection = BACKWARD;
  else M2Mdirection = FORWARD;

  // comment out to speed up PID loop
  //  Serial.print("PWMOutput= ");
  //  Serial.println(PWMOutput,DEC);

}

//---------DC MOTOR 3 CONTROL----------
void movePID3(long location){
  Serial.println(updatePos3());
  GetError3(location);       // Get position error
  CalculatePID3();   // Calculate the PID output from the error
  moveMotor3 (M3Mdirection,M3PWMOutput,0);

}
long updatePos3 (){
  long newPosition3 = M3Enc.read();
  if (newPosition3 != M3oldPosition) {
    M3oldPosition = newPosition3;
  }
  return newPosition3;
}
void moveMotor3(int Mdirection, int PWM_val, long tick)  {
  //  countInit = count;    // abs(count)
  //  tickNumber = tick;
  if(Mdirection==FORWARD)          motor3Forward(PWM_val);
  else if(Mdirection==BACKWARD)    motor3Backward(PWM_val);
}

void motor3Forward(int PWM_val)  {
  analogWrite(M3PWM1, PWM_val);
  digitalWrite(M3InA1, LOW);
  digitalWrite(M3InB1, HIGH);
  //  run = true;
}

void motor3Backward(int PWM_val)  {
  analogWrite(M3PWM1, PWM_val);
  digitalWrite(M3InA1, HIGH);
  digitalWrite(M3InB1, LOW);
  //  run = true;
}

void motor3Brake()  {
  analogWrite(M3PWM1, 0);
  digitalWrite(M3InA1, HIGH);
  digitalWrite(M3InB1, HIGH);
  //  run = false;
}

void GetError3(long DesiredPosition)
{
  byte i = 0;
  // read analogs
  long ActualPosition = updatePos3();  
  // comment out to speed up PID loop
  //  Serial.print("ActPos= ");
  //  Serial.println(ActualPosition,DEC);

  //  long DesiredPosition = 1000;
  // comment out to speed up PID loop
  //  Serial.print("DesPos= ");
  //  Serial.println(DesiredPosition,DEC);

  // shift error values
  for(i=0;i<10;i++)
    M3Error[i+1] = M3Error[i];
  // load new error into top array spot  
  M3Error[0] = (long)DesiredPosition-(long)ActualPosition;
  // comment out to speed up PID loop
  //  Serial.print("Error= ");
  //  Serial.println(Error[0],DEC);

}

/* CalculatePID():
 Error[0] is used for latest error, Error[9] with the DTERM
 */
void CalculatePID3(void)
{
  // Set constants here
  M3PTerm = 100;
  M3ITerm = 25;
  M3DTerm = 10;
  M3Divider = 10;

  // Calculate the PID  
  M3PID = M3Error[0]*M3PTerm;     // start with proportional gain
  M3Accumulator += M3Error[0];  // accumulator is sum of errors
  M3PID += M3ITerm*M3Accumulator; // add integral gain and error accumulation
  M3PID += M3DTerm*(M3Error[0]-M3Error[9]); // differential gain comes next
  M3PID = M3PID>>M3Divider; // scale PID down with divider

    // comment out to speed up PID loop  
  //Serial.print("PID= ");
  //  Serial.println(PID,DEC);

  // limit the PID to the resolution we have for the PWM variable

  if(M3PID>=MAX_DC_SPEED)
    M3PID = MAX_DC_SPEED;
  if(M3PID<=-MAX_DC_SPEED)
    M3PID = -MAX_DC_SPEED;

  //PWM output should be between 1 and 254 so we add to the PID    
  M3PWMOutput = abs(M3PID);
  if (M3PID<0) M3Mdirection = BACKWARD;
  else M3Mdirection = FORWARD;

  // comment out to speed up PID loop
  //  Serial.print("PWMOutput= ");
  //  Serial.println(PWMOutput,DEC);

}


void testRun(){
  moveAct(Act1In1Pin,Act1In2Pin,Act1EnbPin,false,1000);
  moveAct(Act2In1Pin,Act2In2Pin,Act2EnbPin,true,1000);

}










