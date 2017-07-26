package ir.mtajik.android.advancedsmsmanager.model;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.widget.Toast;


public class MySmsManager {


    Context context;
    SMSManagerCallBack mySmsManagerCallBack = null;
    private String SENT = "SMS_SENT";
    private String DELIVERED = "SMS_DELIVERED";
    private String sms_number;
    private String mBody;
    private int carrierSlotNumber;
    private int carrierSlotCount;
    private int smsId = 0;
    private String carrierName;
    private String carrierNameFilter;


    public MySmsManager(String sms_number, Context context) {

        this.context = context;
        this.sms_number = sms_number;

    }

    public void generateSMS(int smsId, String body, SMSManagerCallBack callBack) {

        this.mySmsManagerCallBack = callBack;
        this.smsId = smsId;
        this.mBody = body;
        sendSMS(mBody);


    }

    public void generateSMS(int smsId, String body, int carrierSlotCount, int carrierSlutNumber,
                            String carrierName,
                            SMSManagerCallBack callBack) {

        this.mySmsManagerCallBack = callBack;
        this.smsId = smsId;

        initializeSmsManager(body, carrierSlotCount, carrierSlutNumber, carrierName);
        sendSMS(mBody);


    }

    public void initializeSmsManager(String body, int carrierSlotCount, int carrierSlotNumber,
                                     String
            cariername) {
        this.carrierSlotNumber = carrierSlotNumber;
        this.carrierName = cariername;
        this.mBody = body;
        this.carrierSlotCount = carrierSlotCount;
    }

    public void sendSMS(String message) {

        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                new Intent(DELIVERED), 0);

        BroadcastReceiver mySender = new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {

                    case Activity.RESULT_OK:
                        context.unregisterReceiver(this);
                        if (mySmsManagerCallBack != null)
                            mySmsManagerCallBack.afterSuccessfulSMS(smsId);
                        break;

                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(context, "خطا در ارسال", Toast.LENGTH_SHORT).show();
                        context.unregisterReceiver(this);
                        if (mySmsManagerCallBack != null)
                            mySmsManagerCallBack.afterUnSuccessfulSMS(smsId, "خطا در ارسال");
                        break;

                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        context.unregisterReceiver(this);
                        if (mySmsManagerCallBack != null)
                            mySmsManagerCallBack.afterUnSuccessfulSMS(smsId, "عدم وجود سرویس شبکه");
                        break;

                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        context.unregisterReceiver(this);
                        if (mySmsManagerCallBack != null)
                            mySmsManagerCallBack.afterUnSuccessfulSMS(smsId, " PDU خطای ");
                        break;

                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        context.unregisterReceiver(this);
                        if (mySmsManagerCallBack != null)
                            mySmsManagerCallBack.afterUnSuccessfulSMS(smsId, "عدم وجود ارتباط " +
                                    "رادیویی");
                        break;

                }
            }
        };

        context.registerReceiver(mySender, new IntentFilter(SENT));

        BroadcastReceiver myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        context.unregisterReceiver(this);
                        mySmsManagerCallBack.afterDelivered(smsId);

                        break;
                    case Activity.RESULT_CANCELED:
                        context.unregisterReceiver(this);
                        if (mySmsManagerCallBack != null)
                            mySmsManagerCallBack.afterUnSuccessfulSMS(smsId, "پیامک به مقصد نرسید");

                        break;

                }
            }
        };
        context.registerReceiver(myReceiver, new IntentFilter(DELIVERED));
        SmsManager sms = SmsManager.getDefault();
        int subscriptionId = carrierSlotNumber;

        //even selected carrier Slot is 0 if there is more than one carier ( means 2 sim) it shoud
        // use first method to send sms.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1
                && carrierSlotCount > 1) {

            if (checkCarrierNameFilter()) {
                sms.getSmsManagerForSubscriptionId(subscriptionId).sendTextMessage(sms_number,
                        null, message, sentPI, deliveredPI);

            } else {
                mySmsManagerCallBack.onCarrierNameNotMatch(smsId, "you can not send sms for this " +
                        "app from carrier:" + carrierName);
            }
        } else {
            sms.sendTextMessage(sms_number, null, message, sentPI, deliveredPI);
        }

    }

    private boolean checkCarrierNameFilter() {
        if (carrierNameFilter != null) {
            if (carrierName.contains(carrierNameFilter)) {
                return true;
            } else {
                return false;
            }

        } else {
            return true;
        }
    }

    public void setSms_number(String sms_number) {
        this.sms_number = sms_number;
    }

    public void setCarrierNameFilter(String carrierNameFilter) {
        this.carrierNameFilter = carrierNameFilter;
    }

    public interface SMSManagerCallBack {
        void afterSuccessfulSMS(int smsId);

        void afterDelivered(int smsId);

        /*
         SmsManager error types (from 1 ~5) and Activity.RESULT_CANCELED = 0
         */
        void afterUnSuccessfulSMS(int smsId, String message);

        void onCarrierNameNotMatch(int smsId, String message);

    }


}