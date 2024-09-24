package frc.robot.subsystems;

import com.revrobotics.CANSparkLowLevel.MotorType;
import com.ctre.phoenix6.hardware.CANcoder;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.util.Units;

import frc.robot.subsystems.AbsoluteEncoder.EncoderConfig;

public class SwerveModule {
    // CONSTANTS
    private static final double TAU = Math.PI * 2; // full circle
    private static final double MAX_SPEED_METERS_PER_SECOND = Units.feetToMeters(14);
    // PID Values
    private static final double P = 0.05;
    // Gear Ratio
    private static final double ANGLE_MOTOR_GEAR_RATIO = (14.0 / 50.0) * (10.0 / 60.0);

    // Motors and Encoders
    private final CANSparkMax driveMotor;
    private final CANSparkMax angleMotor;

    private final RelativeEncoder driveMotorRelativeEncoder; // rotations
    private final RelativeEncoder angleMotorRelativeEncoder; // radians

    private final CANcoder absoluteEncoder;
    private final EncoderConfig encoderConfig;

    private final double turnAngleRadians;

    public SwerveModule(int driveMotorDeviceId, int angleMotorDeviceId, Translation2d location, EncoderConfig config) {
        driveMotor = new CANSparkMax(driveMotorDeviceId, MotorType.kBrushless);
        angleMotor = new CANSparkMax(angleMotorDeviceId, MotorType.kBrushless);

        turnAngleRadians = getTurningAngleRadians(location); // only used for alternative swerve

        driveMotorRelativeEncoder = driveMotor.getEncoder();
        angleMotorRelativeEncoder = angleMotor.getEncoder();
        angleMotorRelativeEncoder.setPositionConversionFactor(TAU); // converts rotations to radians

        encoderConfig = config;
        absoluteEncoder = AbsoluteEncoder.createAbsoluteEncoder(config);

        resetEncoders();
    }

    private static double getTurningAngleRadians(Translation2d location) {
        double turningAngleRadians = (Math.PI / 2) - getAngleRadiansFromComponents(location.getY(), location.getX());
        return normalizeAngleRadians(turningAngleRadians);
    }

    public void setDriveMotorSpeed(double speed) {
        driveMotor.set(normalizeSpeed(speed));
    }

    public void setState(SwerveModuleState state) {
        double speedMetersPerSecond = state.speedMetersPerSecond;
        setAngle(state.angle);
        setDriveMotorSpeed(speedMetersPerSecond / MAX_SPEED_METERS_PER_SECOND);
    }

    public void setState(double driveSpeed, double driveAngleRadians, double turnSpeed) {
        double[] desiredState = getDesiredState(driveAngleRadians, turnAngleRadians, driveSpeed, turnSpeed);
        double desiredAngleRadians = desiredState[0];
        double desiredSpeed = desiredState[1];
        setAngle(Rotation2d.fromRadians(desiredAngleRadians));
        setDriveMotorSpeed(desiredSpeed);
    }

    private static double[] getDesiredState(double driveAngleRadians, double turnAngleRadians, double driveSpeed, double turnSpeed) {
        // Get x and y components of speeds
        double driveSpeedY = driveSpeed * Math.sin(driveAngleRadians);
        double driveSpeedX = driveSpeed * Math.cos(driveAngleRadians);
        double turnSpeedY = turnSpeed * Math.sin(turnAngleRadians);
        double turnSpeedX = turnSpeed * Math.cos(turnAngleRadians);
        // Get total speeds in x and y directions
        double speedY = driveSpeedY + turnSpeedY;
        double speedX = driveSpeedX + turnSpeedX;
        // Determine and return angle and total speed
        double desiredAngle = Math.atan2(speedY, speedX);
        double speed = normalizeSpeed(Math.hypot(speedX, speedY));
        return new double[] {desiredAngle, speed};
    }

    private static double convertRadiansToSpeed(double errorRadians) {
        return normalizeSpeed(errorRadians / Math.PI * P);
    }

    // Limits the speed to -1 to 1
    private static double normalizeSpeed(double speed) {
        if (speed > 1) {
            return 1;
        } else if (speed < -1) {
            return -1;
        }
        return speed;
    }

    public double getAngleMotorRadians() {
        return angleMotorRelativeEncoder.getPosition();
    }

    public void setAngle(Rotation2d desiredAngle) {
        double currentWheelAngleRadians = normalizeAngleRadians(getAngleMotorRadians() * ANGLE_MOTOR_GEAR_RATIO);
        double desiredWheelAngleRadians = normalizeAngleRadians(desiredAngle.getRadians());
        double wheelErrorAngleRadians = normalizeAngleRadians(desiredWheelAngleRadians - currentWheelAngleRadians);
        // Optimizes the angle, goes shortest direction
        // If the error is greater than 180 degrees, go the other direction
        if (wheelErrorAngleRadians > Math.PI) {
            wheelErrorAngleRadians = -(TAU - wheelErrorAngleRadians);
        }
        // Convert wheel radians to motor radians
        double motorErrorRadians = wheelErrorAngleRadians / ANGLE_MOTOR_GEAR_RATIO;
        // Convert radians to speed
        double speed = convertRadiansToSpeed(motorErrorRadians);
        angleMotor.set(speed);
    }

    public double getEncoderValue() {
        return getAngleMotorRadians() / TAU; // convert radians to rotations
    }

    public static double getAngleRadiansFromComponents(double y, double x) {
        return normalizeAngleRadians(Math.atan2(y, x));
    }

    // Get the coterminal angle between 0 and tau (360 degrees)
    public static double normalizeAngleRadians(double angleRadians) {
        while (angleRadians < 0 || angleRadians > TAU) {
            angleRadians += angleRadians < 0 ? TAU : -TAU;
        }
        return angleRadians;
    }

    public double getAbsoluteEncoderRadians() {
        return absoluteEncoder.getAbsolutePosition().getValueAsDouble() * TAU - encoderConfig.getOffset();
    }

    public void resetEncoders() {
        driveMotorRelativeEncoder.setPosition(0);
        angleMotorRelativeEncoder.setPosition(getAbsoluteEncoderRadians());
    }
}

