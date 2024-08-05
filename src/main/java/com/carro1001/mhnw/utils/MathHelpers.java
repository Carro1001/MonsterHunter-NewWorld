package com.carro1001.mhnw.utils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MathHelpers {

    public static Vec2 angleTo(Vec3 target, Vec3 mePos) {
        double d0 = target.x - mePos.x;
        double d1 = target.y - mePos.y;
        double d2 = target.z - mePos.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        double XAngle = (Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * 57.2957763671875))));
        double YAngle = (Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F));

        return new Vec2((float) XAngle, (float) YAngle);
        //returns the y and x angle from the source location(mePos) to the target location(target)
        //Y is yaw, X is pitch
    }

    public static Vec3 rotateAroundCenterFlatDeg(Vec3 center, Vec3 me, Double angleInDeg) {
        //Rotates me around center, and sets the y coordinate to the coordinate of the center. The intake is in degrees.

        angleInDeg = -(Mth.DEG_TO_RAD*angleInDeg);

        double x1 = me.x - center.x;
        double z1 = me.z - center.z;

        double x2 = (x1 * Math.cos(angleInDeg) - z1 * Math.sin(angleInDeg));
        double z2 = (x1 * Math.sin(angleInDeg) + z1 * Math.cos(angleInDeg));

        double newMeX = (x2 + center.x);
        double newMeZ = (z2 + center.z);

        return new Vec3(newMeX, me.y, newMeZ);
    }

    public static Vec3 rotateAroundCenter3dDeg(Vec3 center, Vec3 me, float yRot, float xRot) {
        //Rotates me around center in 3 dimensions. The intake is in degrees.

        yRot = -(Mth.DEG_TO_RAD*yRot);
        xRot = -(Mth.DEG_TO_RAD*xRot);

        double x1 = me.x - center.x;
        double z1 = me.z - center.z;
        double y1 = me.y - center.y;

        double x2 = (x1 * Math.cos(yRot) - z1 * Math.sin(yRot));
        double z2 = (x1 * Math.sin(yRot) + z1 * Math.cos(yRot));
        double y2 = -(z1 * Math.sin(xRot) + y1 * Math.cos(xRot));

        double newMeX = (x2 + center.x);
        double newMeZ = (z2 + center.z);
        double newMeY = (y2 + center.y);

        return new Vec3(newMeX, newMeY, newMeZ);
    }

    public static double angleFromYdiff(double hyp, Vec3 point, Vec3 child) {
        double ydiff = child.y - point.y;

        return Math.asin(ydiff/hyp);
    }

    public static double getAngleForLinkTopDownFlat(Vec3 point, Vec3 parent, Vec3 child, Vec3 leftRef, Vec3 rightRef){
        //I AM PRETTY SURE LEFT IS NEGATIVE(down) BUT I AM TOO LAZY TO CONFIRM
        //basically this calculates the angle a tail bone that corresponds to a physics node must be set to(horizontal angle)
        //This only need to be done for the links that represent bones, links without corresponding bones(such as the link at the tip of the tail and at the head) does not need this run.
        //Since every link in between has a parent and child, an angle can be calculated.
        //If the bone's CHILD is closer in distance to the left ref, it is a negative angle, otherwise it is a positive angle.
        //Top down in this context means from parent to child

        //REMEMBER THE ENTITY'S POSITION IS ALSO A VALID BONE

        double C = Math.hypot(Math.abs(parent.x - child.x), Math.abs(parent.z - child.z));
        //distance from the parent to the link we're looking for to the child to the link we're looking for
        //hypotenuse because the flat 2d distance is just the hypotenuse of a right triangle where the other sides are the distance in x and z coords
        double A = Math.hypot(Math.abs(parent.x - point.x), Math.abs(parent.z - point.z));
        //distance from the point we're looking for to its parent
        double B = Math.hypot(Math.abs(point.x - child.x), Math.abs(point.z - child.z));
        //distance from the point we're looking for to its child

        double distToLeft = Math.abs(Math.hypot(child.x - leftRef.x, child.z - leftRef.z));
        double distToRight = Math.abs(Math.hypot(child.x - rightRef.x, child.z - rightRef.z));

        double c = Math.acos(((A*A)+(B*B)-(C*C))/(2*A*B));

        if (distToLeft >= distToRight) {
            //closer to right
            return c;
        } else {
            //closer to left
            return -c;
        }

    }


}
