package com.example.tutornearme;

import com.example.tutornearme.Model.TutorInfoModel;

public class CommonClass {
    public static final String TUTOR_INFO_REFERENCE = "TutorInfo";
    public static final String TUTORS_LOCATION_REFERENCE = "TutorsLocation" ;
    public static TutorInfoModel currentUser;

    public static String buildWelcomeMessage(){
        if (currentUser != null){
            return new StringBuilder()
                    .append(CommonClass.currentUser.getFirstName())
                    .append(" ")
                    .append(CommonClass.currentUser.getLastName()).toString();
        }else {
            return "";
        }
    }
}
