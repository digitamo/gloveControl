#include<Wire.h>


const int MPU=0x68;  // I2C address of the MPU-6050
int16_t AcX,AcY,AcZ,Tmp,GyX,GyY,GyZ;
int XFlexPin = 0;
int YFlexPin = 1;
int accRatio = 100;
int accMax = 2500;
int resX = 1366;
int resY = 768;

// TODO: use JSON library.

void setup()
{
	Wire.begin();
	Wire.beginTransmission(MPU);
  Wire.write(0x6B);  // PWR_MGMT_1 register
  Wire.write(0);     // set to zero (wakes up the MPU-6050)
  Wire.endTransmission(true);
  Serial.begin(9600);
}


void loop()
{

// Read values

Wire.beginTransmission(MPU);
  Wire.write(0x3B);  // starting with register 0x3B (ACCEL_XOUT_H)
  Wire.endTransmission(false);
  Wire.requestFrom(MPU,14,true);  // request a total of 14 registers
  AcX=Wire.read()<<8|Wire.read();  // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)     
  AcY=Wire.read()<<8|Wire.read();  // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
  AcZ=Wire.read()<<8|Wire.read();  // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
  Tmp=Wire.read()<<8|Wire.read();  // 0x41 (TEMP_OUT_H) & 0x42 (TEMP_OUT_L)
  GyX=Wire.read()<<8|Wire.read();  // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
  GyY=Wire.read()<<8|Wire.read();  // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
  GyZ=Wire.read()<<8|Wire.read();  // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)
  int XFlexVal = analogRead(XFlexPin);
  int YFlexVal = analogRead(YFlexPin);


// apply the ratio values.

AcX = AcX / accRatio;
AcY = AcY / accRatio;
AcZ = AcZ / accRatio;
Tmp = Tmp / accRatio;
GyX = GyX / accRatio;
GyY = GyY / accRatio;
GyZ = GyZ / accRatio;

// Create the String in JSON Format.


  String text = "{\"XFlex\" : ";
  String Json = text + XFlexVal;
  text = ", \"YFlex\" : ";
  String string = text + YFlexVal;
  Json += string;
  text = ", \"AcX\" : ";
  string = text + AcX;
  Json += string;
  text = ", \"AcY\" : ";
  string = text + AcY;
  Json += string;
  text = ", \"AcZ\" : ";
  string = text + AcZ;
  Json += string;
  text = ", \"Tmp\" : ";
  string = text + Tmp;
  Json += string;
  text = ", \"GyX\" : ";
  string = text + GyX;
  Json += string;
  text = ", \"GyY\" : ";
  string = text + GyY;
  Json += string;
  text = ", \"GyZ\" : ";
  string = text + GyZ;
  Json += string;

  string = "}";
  Json += string;


  Serial.println(Json);

  // Send the data Every 300 ms
  // delay(300);
}


