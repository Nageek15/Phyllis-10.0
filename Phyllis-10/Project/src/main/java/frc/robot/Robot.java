/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.buttons.*;
import frc.robot.motors.Arm;
import frc.robot.motors.Elevator;
import frc.robot.motors.ExtendableMotor;
import frc.robot.motors.Iotake;
import frc.robot.motors.Lift;

import com.revrobotics.*;

import java.util.Hashtable;

import com.ctre.phoenix.motorcontrol.*;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

public class Robot extends TimedRobot {
  
  
  //CLASS VARIABLES

  public static enum driveType {twoJoyStick,oneJoystick};
  private boolean debugging;
  private final double voltageCutoff=8.5;
  private DifferentialDrive robot;
  private Joystick leftStick;
  private Joystick rightStick;
  private Joystick tractorPanel;
  private Joystick xStick;
  private JSBAdapter jsbAdapter;
  private TSBAdapter tsbAdapter;
  private Solenoid solenoid;
  private Compressor compressor;
  private CANSparkMax leftFrontCAN=new CANSparkMax(10,CANSparkMaxLowLevel.MotorType.kBrushless);
  private CANSparkMax leftBackCAN=new CANSparkMax(11,CANSparkMaxLowLevel.MotorType.kBrushless);
  private CANSparkMax rightFrontCAN=new CANSparkMax(13,CANSparkMaxLowLevel.MotorType.kBrushless);
  private CANSparkMax rightBackCAN=new CANSparkMax(12,CANSparkMaxLowLevel.MotorType.kBrushless);
  private Elevator elevator=new Elevator();
  private Iotake iotake=new Iotake();//Iotake is intake/outtake system
  private Arm arm=new Arm();
  private Lift rearLift=new Lift();
  private Hashtable<String,Integer> tuningValues;
  //private ExtendableMotor extendableintakeRight = new ExtendableMotor(intakeRight, 0.05, 1);
  

  //ROBOT ESSENTIAL METHODS

  @Override
  public void robotInit() { 
    debugging=false;
    CameraServer.getInstance().startAutomaticCapture();  
    leftBackCAN.follow(leftFrontCAN);
    rightBackCAN.follow(rightFrontCAN);
    robot = new DifferentialDrive(leftFrontCAN,rightFrontCAN);
    leftStick = new Joystick(0);
    rightStick = new Joystick(1);
    xStick = leftStick;
    tractorPanel = new Joystick(2);
    jsbAdapter=new JSBAdapter(rightStick, this);
    tsbAdapter=new TSBAdapter(tractorPanel, this);
    tuningValues=new Hashtable<>();
    tuningValues.put("eTop",10);
    tuningValues.put("eBot",10);
    tuningValues.put("eMid",10);
    tuningValues.put("eDec",10);
    tuningValues.put("aHat",10);
    tuningValues.put("aBal",10);
    tuningValues.put("aSit",10);
    tuningValues.put("aDec",10);
    tuningValues.put("lTop",10);
    tuningValues.put("lBot",10);

    //finish tuning values
    compressor=new Compressor(0); //DOUBLE CHECK IDS
  }
  //WARNING: Comment this out after test, just here to see if the robot can drive while "disabled"
  @Override
  public void disabledPeriodic() {
    periodic();
  }

  @Override
  public void teleopPeriodic() {
    periodic();
  }
  @Override
  public void autonomousPeriodic() {
    super.autonomousPeriodic();
    periodic();
  }
  private void periodic(){
    //driving arcade
    robot.arcadeDrive(rightStick.getY()*-.6, xStick.getX()*.6);
    
    //update button inputs
    if (!(jsbAdapter.equals(null)&&tsbAdapter.equals(null))){
      jsbAdapter.update();
      tsbAdapter.update();
    }
    //stop elevator at voltage dip
    //currently testing at 500 milliseconds (.5 seconds) after motor activation/directional change invocation 
    if ((((elevator.getState()==Elevator.State.activeUp||elevator.getState()==Elevator.State.activePID)&&Math.abs(elevator.getOutputCurrent())>30)||(elevator.getState()==Elevator.State.activeDown&&elevator.getOutputCurrent()>2.5)) && System.currentTimeMillis()>elevator.getActivationTime()+250){
      elevatorOff();
      System.out.println("Elevator disabled from high output current");
    }
    if (!(rearLift.getState()==Lift.State.off)&&Math.abs(rearLift.getMotorOutputVoltage())<voltageCutoff && System.currentTimeMillis()>rearLift.getActivationTime()+500){
      liftOff();
      System.out.println("Rearlift disabled from low output voltage");
    }
    if (!(arm.getState()==Arm.State.off)&&Math.abs(arm.getOutputCurrent())>17 && System.currentTimeMillis()>arm.getActivationTime()+500){
      armOff();
      System.out.println("Arm disabled from high output current");
    }
    //stop intake at voltage dip
    //currently testing at 500 milliseconds (.5 seconds) after motor activation/directional change invocation
    //no abs value needed for this one because outtake doesn't need to be ended at a voltage spike
    if (iotake.getState()==Iotake.State.activeIn&&iotake.getOutputCurrent()>20 && System.currentTimeMillis()>iotake.getActivationTime()+500){
      iotakeOff();
      System.out.println("Outtake disabled from high output current");
    }
    if (debugging){
      debug();
    }
  }
  
  
  //TUNING PROPERTIES

  public void setProp(String name,int value){
    tuningValues.replace(name,value);
  }

  public int getProp(String name){
    return tuningValues.get(name);
  }


  //DRIVE TYPE

  /**Set drive type e.g. <code>driveType.oneJoystick</code> for one joystick arcade drive
   * 
   * @param d
   */
  public void setDriveType(driveType d){
    if (d==driveType.oneJoystick){
      xStick=rightStick;
    } else {
      xStick=leftStick;
    }
  }


  /**Returns current drive type
   * 
   * @return
   */
  public driveType getDriveType(){
    if (xStick.equals(rightStick)){
      return driveType.oneJoystick;
    } else {
      return driveType.twoJoyStick;
    }
  }


  //PNUEMATICS

  /**Activates/deactivates solenoid to fire hatch*/
  public void fireHatch(boolean on){
    solenoid.set(on);
  }
  /**Toggle compressor
   * 
   */
  public void toggleCompressor(){
    compressor.setClosedLoopControl(!compressor.enabled());
  }


  //ELEVATOR CONTROL METHODS

  /**Moves elevator up
   * 
   */
  public void elevatorUp(){
    elevator.up(.85);
  }

  /**Moves elevator down
   * 
   */
  public void elevatorDown(){
    elevator.down(.5);
  }
  //Not fuctional!
  public void elevatorTop(){
    elevator.moveToPos(tuningValues.get("eTop"),.1);
  }

  public void elevatorBottom(){
    elevator.moveToPos(tuningValues.get("eBot"),.1);
  }
  
  /**Moves elevator to middle possition
   * NOT FUCTIONAL
   * <--NEEDS TO HAVE VALUE TUNED-->
   * 
   */
  public void elevatorMid(){
    elevator.moveToPos(tuningValues.get("eMid"),.1);
  }
  
  /**Moves elevator to deck position
   * NOT FUCTIONAL
   * <--NEEDS TO HAVE VALUE TUNED-->
   * 
   */
  public void elevatorDeck(){
    elevator.moveToPos(tuningValues.get("eDec"),.1);
  }

  /**turns off elevator
   * 
   */
  public void elevatorOff(){
    elevator.off();
  }


  //LIFT CONTROL METHODS

  /**Moves lift up
   * 
   */
  public void liftUp(){
    rearLift.up();
  }
  
  /**Moves lift down
   * 
   */
  public void liftDown(){
    rearLift.down();
  }
  /**Moves lift to raised position
   * 
   */
  public void liftRaise(){
    rearLift.moveToPos(tuningValues.get("lTop"),.3);
  }

  public void liftLower(){
    rearLift.moveToPos(tuningValues.get("lBot"),.3);
  }
  /**Turns lift off
   * 
   */
  public void liftOff(){
    rearLift.off();
  }


  //ARM CONTROL METHODS

  /**Moves arm up
   * 
   */
  public void armUp(){
    arm.up(.5);
  }

  /**Moves arm down
   * 
   */
  public void armDown(){
    arm.down(.5);
  }

  /**Puts arm in sitting position?
   * 
   */
  public void armSit(){
    arm.moveToPos(tuningValues.get("aSit"),.3);
  }

  /**Puts arm at deck height
   * 
   */
  public void armDeck(){
    arm.moveToPos(tuningValues.get("aDec"),.3);
  }

  /**Puts arm in hatch (up) position
   * 
   */
  public void armHatch(){
    arm.moveToPos(Preferences.getInstance().getDouble("aHat",0),.3);
  }
  
  /**Puts arm in ball (down) position
   * 
   */
  public void armBall(){
    arm.moveToPos(Preferences.getInstance().getDouble("aBal",0),.3);
  }
  
  /**Turns arm off
   * 
   */
  public void armOff(){
    arm.off();
  }
  

  //IOTAKE CONTROL METHODS

  /**Activates outtake
   * 
   */
  public void outtake(){
    iotake.outtake(.85);
  }

  /**Initiates intake
   * 
   */
  public void intake(){
    iotake.intake(.75);
  }

  /**Turns iotake off
   * 
   */
  public void iotakeOff(){
    iotake.off();
  }


  //OTHER METHODS

  /**Prints sensor positions for all monitored motors
   * 
   */
  public void printSensorPositions(){
    System.out.println("<--ELEVATOR-->");
    elevator.printSensorPosition();
    System.out.println("<--REAR LIFT-->");
    rearLift.printSensorPosition();
    System.out.println("<--ARM-->");
    arm.printSensorPosition();
  }

  /**Sets wheather the robot should display debug info
   * @param debugging
   */
  public void setDebugging(boolean debugging) {
    this.debugging = debugging;
  }

  /**
   * Prints debug information to console, currently only for debugging purposes (cannot invoke with robot IO)
   * Might make more detailed and advanced debugging methods later, also will probably make write to a log file to better examen debug info (kind of hard to analize a periodically changing console)
   */
  private void debug(){
    //printSensorPositions();
    //System.out.println("Test:"+Preferences.getInstance().getDouble("Test", 0));
    System.out.println(elevator.getOutputCurrent());
    /*System.out.println("X:"+leftStick.getX()); //not necessary right now 
    System.out.println("Y: "+leftStick.getY());
    System.out.println("Left: "+leftFrontCAN.getAppliedOutput());
    System.out.println("Right: "+rightFrontCAN.getAppliedOutput());*/
  }
}
