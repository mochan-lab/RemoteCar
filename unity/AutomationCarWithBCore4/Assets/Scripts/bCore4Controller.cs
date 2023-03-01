using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class bCore4Controller : MonoBehaviour
{
    [SerializeField] Dropdown dropdown;
    [SerializeField] Text logText;

    private AndroidJavaObject androidbCore4 = null;
    private AndroidJavaObject myContext = null;

    private Dictionary<string, string> bleDevices = new Dictionary<string, string>();   // address -> name
    private Dictionary<int, string> dropdownOptions = new Dictionary<int, string>();    // optionValue -> address

    private int BCore4Voltage = 0;

    private string status;
    private int gotChara = 0;

    private sbyte[] command = new sbyte[]{0, 0, -127, 0, 0, 0, 0};

    // Start is called before the first frame update
    void Start()
    {
#if UNITY_ANDROID
        androidbCore4 = new AndroidJavaObject("com.mochan.unitynativepluginbcore4.AndroidBLEbCore4");
        AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        myContext = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
        androidbCore4.Call("initialize");
        androidbCore4.Call("scanDevice", myContext, true);

#elif UNITY_EDITOR
        UnityEngine.Debug.LogError("Please running on Android!!");
#endif
    }

    // Update is called once per frame
    void Update()
    {
        
    }

    public void ConnectToDevice()
    {
        int optionKey = dropdown.value;
        if(dropdownOptions.GetValueOrDefault(optionKey) == null)
        {
            return; // Not exist the key
        }
        androidbCore4.Call("connectToDevice", myContext, dropdownOptions[optionKey]);
        //StartCoroutine(GetBatteryVoltage());
    }

    public IEnumerator startDiscoverService(){
        while (true)
        {
            yield return new WaitForSeconds(1);
            if(status=="connected"){
                androidbCore4.Call("discoverService");
            }
            if(status=="gotChara"){
                StartCoroutine(AlterBurst());
                break;
            }
        }
    }

    public IEnumerator GetBatteryVoltage()
    {
        while (true)
        {
            yield return new WaitForSeconds(1);
            androidbCore4.Call("getBatteryVoltage");
        }
    }

    public IEnumerator AlterBurst(){
        
        while(true){
            androidbCore4.Call("getBatteryVoltage");
            yield return new WaitForSeconds(0.1f);
            BurstCommand(command);
            yield return new WaitForSeconds(0.1f);
        }
    }

    public void BurstCommand(sbyte[] command){
        androidbCore4.Call("burstCommand", command);
    }

    public void Led1Switch(){
        if (command[2] == -128){
            command[2] = -127;
        } else if(command[2] == -127){
            command[2] = -128;
        }
    }

    public void Move2Motors(Vector2 motors){
        // float m0=0f;
        // if(motors[0]>=0){
        //     m0=127-(motors[0]*127);
        // }else if(motors[0]<0){
        //     m0=-128-(motors[0]*127);
        // }
        // command[0] = System.Convert.ToSByte((int)m0);
        command[0] = System.Convert.ToSByte((int)motors[0]);
        // float m1=0f;
        // if(motors[1]>=0){
        //     m1=127-(motors[1]*127);
        // }else if(motors[1]<0){
        //     m1=-128-(motors[1]*127);
        // }
        // command[1] = System.Convert.ToSByte((int)m1);
        command[1] = System.Convert.ToSByte((int)motors[1]);
        
    }

    /// <summary>
    /// Plugin called these functions.
    /// </summary>
    public void MessageFromPlugin(string str)
    {
        Debug.Log(str);
        if(str== "MESSAGE,Connected device")
        {
            status = "connected";
            StartCoroutine(startDiscoverService());
        }
        if(str=="MESSAGE,Discover GET_BATTERY_VOLTAGE"){
            gotChara ++;
        }
        if(str=="MESSAGE,Discover BURST_COMMAND"){
            gotChara ++;
        }
        if(gotChara >= 2){
            status = "gotChara";
            // StartCoroutine(GetBatteryVoltage());
        }
    }

    public void DeviceFromPlugin(string str)
    {
        Debug.Log(str);
        string[] messages = str.Split(',');
        if (messages.Length != 2)
        {
            return; // seem it not device -> address
        }
        if(bleDevices.GetValueOrDefault(messages[1]) != null)
        {
            return; // already exist
        }
        // update dropdown options
        bleDevices.Add(messages[1], messages[0]);
        Dropdown.OptionData optionData = new Dropdown.OptionData(messages[0]);
        dropdown.options.Add(optionData);
        dropdownOptions.Add(dropdown.options.Count - 1, messages[1]);
    }

    public void ReturnBatteryVoltage(string str)
    {
        Debug.Log(str);
        BCore4Voltage = int.Parse(str);
        logText.text = str;
    }
}
