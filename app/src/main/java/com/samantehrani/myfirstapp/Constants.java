package com.samantehrani.myfirstapp;



public class Constants {
    public interface ACTION {
        public static String MAIN_ACTIVITY = "com.samantehrani.myfirstapp.action.main";
        public static String START_SERVICE = "com.samantehrani.myfirstapp.action.start";
        public static String REFRESH_SERVICE = "com.samantehrani.myfirstapp.action.refresh";
        public static String STOP_SERVICE = "com.samantehrani.myfirstapp.action.stop";
        public static String SEND_MSG = "com.samantehrani.myfirstapp.action.send";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
    public interface BLUETOOTH_STATE{
        public static String BL_DOWN = "com.samantehrani.myfirstapp.state.bdown";
        public static String BL_UP = "com.samantehrani.myfirstapp.state.bup";
    }
    public interface INTERNET_STATE{
        public static String INTERNET_DOWN = "com.samantehrani.myfirstapp.state.idown";
        public static String INTERNET_UP = "com.samantehrani.myfirstapp.state.iup";
    }
}