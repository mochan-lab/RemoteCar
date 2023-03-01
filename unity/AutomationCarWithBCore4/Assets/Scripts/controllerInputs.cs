using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.InputSystem;

public class controllerInputs : MonoBehaviour
{
    public bCore4Controller bcore4controller;
    public GameObject stick;

    private const int rightMotor = 0;
    private const int leftMotor = 1;
    private const float rightMotorMP = 10f;
    private const float leftMotorMP = 0f;

    private bool isZero = false;

    public void OnMove(InputAction.CallbackContext context){
        Vector2 value = context.ReadValue<Vector2>();
        if(!context.performed) return;
        isZero = false;
        float angle = Mathf.Atan2(value.y, value.x);
        float f2 = Mathf.Cos(angle);
        float d = 1 - value.magnitude;
        Vector2 retval = new Vector2(0f,0f);

        if(angle >=0){
            if(angle < Mathf.PI / 2){
                retval[leftMotor] = d * 127;
                retval[rightMotor] = (f2 * 127) * d;
            }else if((angle >= Mathf.PI / 2)){
                retval[rightMotor] = d * 127;
                retval[leftMotor] = (f2 * 127) * d;
            }
            retval[leftMotor] += leftMotorMP;
            retval[rightMotor] += rightMotorMP;
            for(int i = 0;i<2;i++){
                if(retval[i] < 0)retval[i]=0;
                else if(retval[i] > 127)retval[i]=127;
            }
        }else if(angle < 0){
            if(angle >= -(Mathf.PI / 2)){
                retval[leftMotor] = -1 - (d * 127);
                retval[rightMotor] = -1 + (f2 * 127) * d;
            }else if(angle < -(Mathf.PI / 2)){
                retval[rightMotor] = -1 - (d * 127);
                retval[leftMotor] = -1 + (f2 * 127) * d;
            }
            retval[leftMotor] -= leftMotorMP;
            retval[rightMotor] -= rightMotorMP;
            for(int i = 0;i<2;i++){
                if(retval[i] > -1)retval[i]=-1;
                else if(retval[i] < -128)retval[i]=128;
            }
        }

            // if(angle >= 0 && angle < Mathf.PI / 2){
            //     retval[leftMotor] = 1f;
            //     retval[rightMotor] = 1 - f2;
            // }else if(angle >= Mathf.PI / 2){
            //     retval[leftMotor] = 1 + f2;
            //     retval[rightMotor] = 1f;
            // }else if(angle < 0 && angle >= -(Mathf.PI / 2)){
            //     retval[leftMotor] = -1f;
            //     retval[rightMotor] = f2 - 1;
            // }else if(angle < -(Mathf.PI / 2)){
            //     retval[leftMotor] = -(1 + f2);
            //     retval[rightMotor] = -1f;
            // }
        bcore4controller.Move2Motors(retval);
        Debug.Log(angle + " : " + retval);
    }

    public void OnFire(InputAction.CallbackContext context){
        if (!context.performed) return;
        bcore4controller.Led1Switch();
        Debug.Log("LED switch");
    }

    void Update(){
        if(isZero==false){
            if(stick.transform.localPosition==new Vector3(0f,0f,0f)){
                Vector2 retval = new Vector2(-128,-128);
                bcore4controller.Move2Motors(retval);
                isZero=true;
                Debug.Log("Zero reset : " + retval);
            }
        }
    }
}
