package net.betrayd.webspeak.util;

import com.google.gson.annotations.SerializedName;

/**
 * Config parameters to send to the client about how to handle audio spatialization.
 */
public class PannerOptions {
    public static enum PanningModelType {
        @SerializedName("HRTF")
        HRTF,
        @SerializedName("equalpower")
        EQUAL_POWER
    }

    public static enum DistanceModelType {
        @SerializedName("inverse")
        INVERSE,
        @SerializedName("exponential")
        EXPONENTIAL,
        @SerializedName("linear")
        LINEAR
    }

    public float coneInnerAngle = 360;
    public float coneOuterAngle = 0;
    public float coneOuterGain = 0;
    public DistanceModelType distanceModel = DistanceModelType.INVERSE;
    public float maxDistance = 26;
    public float orientationX = 0;
    public float orientationY = 0;
    public float orientationZ = 0;
    public PanningModelType panningModel = PanningModelType.HRTF;
    public float positionX = 0;
    public float positionY = 0;
    public float positionZ = 0;
    public float refDistance = 1;
    public float rolloffFactor = 1;

    public void copyFrom(PannerOptions other) {
        this.coneInnerAngle = other.coneInnerAngle;
        this.coneOuterAngle = other.coneOuterAngle;
        this.coneOuterGain = other.coneOuterGain;
        this.distanceModel = other.distanceModel;
        this.maxDistance = other.maxDistance;
        this.orientationX = other.orientationX;
        this.orientationY = other.orientationY;
        this.orientationZ = other.orientationZ;
        this.panningModel = other.panningModel;
        this.positionX = other.positionX;
        this.positionY = other.positionY;
        this.positionZ = other.positionZ;
        this.refDistance = other.refDistance;
        this.rolloffFactor = other.rolloffFactor;
    }
}
