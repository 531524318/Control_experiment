package com.flag.control_experiment.customComponent;

//import android.annotation.TargetApi;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flag.control_experiment.MainActivity;
import com.flag.control_experiment.R;
import com.flag.control_experiment.tcpUtils.IOBlockedRunnable;
import com.flag.control_experiment.tcpUtils.IOBlockedZigbeeRunnable;

import org.json.JSONException;
import org.json.JSONObject;



//import java.util.function.Predicate;
//import java.util.function.ToIntFunction;

/**
 * Created by Bmind on 2018/6/19.
 */

public class NodeLayout extends LinearLayout {
    private static final String TAG = "节点";
    private ImageView wifiswitch;
    private ImageView zigbeeswitch;
    private TextView bindsensor;
    private TextView hiddentext;            //隐藏一些本类需要的信息
    private NodeLayout mThis;
    private LinearLayout nodecontrol;

    public String onlyIp = "";              //唯一标识的该节点
    public String sensorType = "";
    private String linkType;
    private String linkIP;
    private String number;                  //编号
    private View mView;
    private Context mContext;

    private boolean deviceStatue = false; //节点状态，开和关

    public String getLinkIP() {
        return linkIP;
    }

    public String getSensorType() {
        return sensorType;
    }

    public String getLinkType() {
        return linkType;
    }
    public NodeLayout(Context context, AttributeSet attrs) {
        super(context,attrs);
        mContext = context;
        mView = LayoutInflater.from(context).inflate(R.layout.node, this);
        mThis = this;
        wifiswitch = (ImageView) findViewById(R.id.wifiswitch);
        zigbeeswitch = (ImageView) findViewById(R.id.zigbeeswitch);
        bindsensor = (TextView) findViewById(R.id.bindsensor);
        hiddentext = (TextView) findViewById(R.id.hiddentext);
        nodecontrol = (LinearLayout) findViewById(R.id.nodecontrol);
        wifiswitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String jsondata = hiddentext.getText().toString();
                    JSONObject jobject = new JSONObject(jsondata);
                    if(jobject.getString("wifistatue").equals("true")){
                        //组成命令字节,发送控制命令
                        if(((LinearLayout)mThis.getParent()).getId() == R.id.containerLine12        //相等表示在控制节点区域下，所以该点击按钮会发送控制命令
                                || ((LinearLayout)mThis.getParent()).getId() == R.id.containerLine22){
                            String wifiip = jobject.getString("wifiip");
                            String type = jobject.getString("type");
                            switch (type){
                                case "风扇":type = "af";break;
                                /**cssf新增应用**/
                                case "电磁锁":type = "el";break;
                                case "可调灯":type = "al";break;
                                case "继电器":type = "re";break;
                                case "全向红外":type = "or";break;
                                case "声光报警":type = "sl";break;
                            }
                            if(!wifiip.equals("")){
                                IOBlockedRunnable run = (IOBlockedRunnable) MainActivity.socketMap.get(wifiip);
                                if (run == null)return;
                                String order = "Hwc"+type+run.getNumber() + (deviceStatue?"03offT":"02onT");
                                run.pw.write(order);
                                run.pw.flush();
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        zigbeeswitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String jsondata = hiddentext.getText().toString();
                    JSONObject jobject = new JSONObject(jsondata);
                    if(jobject.getString("zigbeestatue").equals("true")){
                        //组成命令字节,发送控制命令
                        if(((LinearLayout)mThis.getParent()).getId() == R.id.containerLine12        //相等表示在控制节点区域下，所以该点击按钮会发送控制命令
                                || ((LinearLayout)mThis.getParent()).getId() == R.id.containerLine22){
                            String zigbeeip = jobject.getString("zigbeeip");
                            String type = jobject.getString("type");
                            switch (type){
                                case "风扇":type = "af";break;
                                /**cssf新增应用**/
                                case "电磁锁":type = "el";break;
                                case "继电器":type = "re";break;
                                case "声光报警":type = "sl";break;
                            }
                            if(!zigbeeip.equals("")){
                                IOBlockedZigbeeRunnable run = (IOBlockedZigbeeRunnable) MainActivity.socketMap.get(zigbeeip);
                                Message msgInfo = new Message();
                                msgInfo.what = 0x110;
                                msgInfo.obj = number + sensorType + "控制中...";
                                handler.sendMessage(msgInfo);
                                zigbeeswitch.setEnabled(false);
//                                handler.sendEmptyMessageDelayed(0x111,5000);
                                Log.d(TAG, "发送控制指令: "+"Hzc"+type+number + (deviceStatue?"03offT":"02onT"));
                                run.pw.write("Hzc"+type+number + (deviceStatue?"03offT":"02onT"));
                                run.pw.flush();
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public JSONObject getHiddenTextJSON(){
        try {
            return new JSONObject(hiddentext.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateHiddenText(String data){          //更新隐藏信息内容
        String[] keyValue = data.split("#");
        if (keyValue.length != 0){
            String jsondata = hiddentext.getText().toString();
            JSONObject jobject = null;
            try {
                //zigbeeip##zigbeestatue#false
                jobject = new JSONObject(jsondata);
                for(int i=0;i < keyValue.length;i++){
                    if (i%2==0){
                        jobject.put(keyValue[i],keyValue[i+1]);
                        if (keyValue[i].equals("wifiip")){
                            if (!keyValue[i+1].equals("")){
                                jobject.put("wifistatue","true");
                                jobject.put("zigbeestatue","false");
                                wifiswitch.setVisibility(VISIBLE);
                                zigbeeswitch.setVisibility(GONE);
                                this.linkType = "wifi";
                                this.linkIP = keyValue[i+1];
                            }else{
                                jobject.put("wifistatue","false");
                                jobject.put("zigbeestatue","true");
                                wifiswitch.setVisibility(GONE);
                                zigbeeswitch.setVisibility(VISIBLE);
                                this.linkType = "zigbee";
                            }
                        }else if(keyValue[i].equals("zigbeeip")){
                            if (!keyValue[i+1].equals("")){
                                jobject.put("zigbeestatue","true");
//                                jobject.put("wifistatue","false");
                                zigbeeswitch.setVisibility(VISIBLE);
                                wifiswitch.setVisibility(GONE);
                                this.linkType = "zigbee";
                                this.linkIP = keyValue[i+1];
                            }else{
                                jobject.put("zigbeestatue","false");
                                jobject.put("wifistatue","true");
                                zigbeeswitch.setVisibility(GONE);
                                wifiswitch.setVisibility(VISIBLE);
                                this.linkType = "wifi";
                            }
                        }
                    }
                }
                sensorType = jobject.getString("type");
                number =  jobject.getString("number");
                hiddentext.setText(jobject.toString());
                if (sensorType.equals("卷帘")){
                }else{
                    bindsensor.setText(number + sensorType);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0x110:
                    String content = msg.obj.toString();
                    bindsensor.setText(content);
                    break;
                case 0x111:
                    zigbeeswitch.setEnabled(true);
                    break;
            }
        }
    };
    //处理返回信息，改变显示状态
    public void setStatueControl(String restr){
        //解析控制节点的状态信息，例Hwdwp0102onT
        String numberData = restr.substring(5,7);
        if (restr.startsWith("H")&&restr.charAt(2)=='d'&&restr.endsWith("T"))//检查是否符合条件
        {
            if (restr.substring(9, 9 + Integer.parseInt(restr.substring(7, 9))).equals("on")) {
                bindsensor.setText(numberData + sensorType + ":开");
                deviceStatue = true;
            } else {
                bindsensor.setText(numberData + sensorType + ":关");
                deviceStatue = false;
            }
            zigbeeswitch.setEnabled(true);
        }
    }


    public int hideWifi(){
        String jsondata = hiddentext.getText().toString();

        IOBlockedZigbeeRunnable run = (IOBlockedZigbeeRunnable) MainActivity.socketMap.get("/"+ MainActivity.ZIGBEE_IP);
        if (run == null){
            wifiswitch.setVisibility(GONE);
            zigbeeswitch.setVisibility(VISIBLE);
            updateHiddenText("wifiip##wifistatue#false");
            return 2;
        }else{
            for (IOBlockedZigbeeRunnable.ZigbeeDevice temp: run.getDeviceList()){
                if (temp.getNumber().equals(number)){
                    Log.d(TAG, "hideWifi: 找到了");
                    return 1;
                }
            }
            return 2;
        }
    }

    public void hideWifiTrue(){
        if (wifiswitch!=null)
        wifiswitch.setVisibility(GONE);
//        updateHiddenText("wifiip##wifistatue#false");
    }

    public int hideZigbee(){         //返回值表示，1 单单移除zigbee， 2 wifi 控制此时也不在线，一块移除
        String jsondata = hiddentext.getText().toString();
        JSONObject jobject = null;
        String wifiip = "";
        try {
            jobject = new JSONObject(jsondata);
            wifiip = jobject.getString("wifiip");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        IOBlockedRunnable run = (IOBlockedRunnable) MainActivity.socketMap.get(wifiip);
        if (run == null){
            wifiswitch.setVisibility(VISIBLE);
            zigbeeswitch.setVisibility(GONE);
            updateHiddenText("zigbeeip##zigbeestatue#false");
            return 2;
        }else{
            return 1;
        }
    }
    public void hideZigbeeTrue(){
        if (zigbeeswitch != null){
            zigbeeswitch.setVisibility(View.GONE);
        }
        updateHiddenText("zigbeeip##zigbeestatue#false");
    }


    public void setImageBackgroundCanClick(){
        wifiswitch.setImageResource(R.drawable.wifipress);
        zigbeeswitch.setImageResource(R.drawable.zigbeepress);
    }
}
