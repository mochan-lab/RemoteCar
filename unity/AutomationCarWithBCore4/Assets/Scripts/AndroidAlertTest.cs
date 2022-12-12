using UnityEngine;
using UnityEngine.UI;

public class AndroidAlertTest : MonoBehaviour
{
    [SerializeField] Text text;
    [SerializeField] Button button;
    [SerializeField] InputField inputField;

    // Start is called before the first frame update
    void Start()
    {
        button.onClick.AddListener(CallAndroidPlugin);
        text.text = "initialize";
    }

    // Update is called once per frame
    void Update()
    {

    }

    /// <summary>
    /// pushed button, show native dialog
    /// </summary>
    public void CallAndroidPlugin()
    {
#if UNITY_ANDROID
        using (AndroidJavaClass nativeDialog = new AndroidJavaClass("com.mochan.unitynativepluginbcore4.AndroidNativeDialog"))
        {
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject context = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            nativeDialog.CallStatic(
                "showNativeDialog",
                context,
                "From Unity",
                inputField.text
                );
        }
#elif UNITY_EDITOR
        UnityEngine.Debug.Log("On Touch!");
#endif
    }

    /// <summary>
    /// Android send message text to Unity
    /// </summary>
    public void CalledFromAndroid(string str)
    {
        text.text = str;
    }
}
