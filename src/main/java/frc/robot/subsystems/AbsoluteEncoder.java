package frc.robot.subsystems;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.MagnetSensorConfigs;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.signals.AbsoluteSensorRangeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;

import frc.robot.Constants;

public class AbsoluteEncoder {

    public enum EncoderConfig {
       /* 
working angles
* ﻿﻿﻿﻿﻿﻿ FL: 10.77183366287412 - 0.50244140625 - 10.7666015625 ﻿
﻿﻿﻿﻿﻿﻿ FR: 10.721383760716662 - 0.499267578125 - 10.698590959821429 ﻿
﻿﻿﻿﻿﻿﻿ BL: 10.775407217847869 - 0.50146484375 - 10.745675223214285 ﻿
﻿﻿﻿﻿﻿﻿ BR: 10.62027024737172 - 0.4970703125 - 10.651506696428571 ﻿
         */

        // Modify these values
        FRONT_LEFT(23, -1.744 + Constants.kTau),
        FRONT_RIGHT(24, 2.0678),
        BACK_LEFT(25, -2.0801 + Math.PI),
        BACK_RIGHT(26, 2.8041);

        private int id;
        private double offset; // in wheel rotations // positive is counterclockwise

        private EncoderConfig(int id, double offset) {
            this.id = id;
            this.offset = offset / (Math.PI * 2);
        }

        public int getId() {
            return id;
        }

        public double getOffset() {
            return offset;
        }
    }

    public static CANcoder createAbsoluteEncoder(EncoderConfig config) {
        int id = config.getId();
        double offset = config.getOffset();

        CANcoder encoder = new CANcoder(id);
        CANcoderConfiguration canConfig = new CANcoderConfiguration();
        MagnetSensorConfigs magConfig = new MagnetSensorConfigs();
        
        magConfig.withAbsoluteSensorRange(AbsoluteSensorRangeValue.Signed_PlusMinusHalf);
        magConfig.withSensorDirection(SensorDirectionValue.Clockwise_Positive);
        magConfig.withMagnetOffset(offset);
        canConfig.withMagnetSensor(magConfig);
        encoder.getConfigurator().apply(canConfig);

        return encoder;
    }
}
