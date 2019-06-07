package com.c5000.mastery.shared;

public class Sanitizer {

    public static final int MIN_SKILL_TITLE_LENGTH = 3;
    public static final int MAX_SKILL_TITLE_LENGTH = 40;
    public static final int MIN_SKILL_DESCRIPTION_LENGTH = 0;
    public static final int MAX_SKILL_DESCRIPTION_LENGTH = 500;

    public static final int MIN_ASSIGNMENT_TITLE_LENGTH = 3;
    public static final int MAX_ASSIGNMENT_TITLE_LENGTH = 40;
    public static final int MIN_ASSIGNMENT_DESCRIPTION_LENGTH = 0;
    public static final int MAX_ASSIGNMENT_DESCRIPTION_LENGTH = 500;

    public static final int MIN_ACTIVITY_TEXT_LENGTH = 0;
    public static final int MAX_ACTIVITY_TEXT_LENGTH = 500;

    public static String common(String in, int minLength, int maxLength, boolean capitalize) {
        if(in == null)
            return null;
        if(in.length() > maxLength || in.length() < minLength)
            return null;
        String result = in;
        result = result.replace("\r", "").replace("\n", " ");
        result = result.trim();
        result = result.replaceAll("\\s+", " ");
        result = capitalize(result);
        if(in.length() > maxLength || in.length() < minLength)
            return null;
        return result;
    }

    public static String capitalize(String result) {
        if(result.startsWith("http"))
            return result;
        if(result.length() == 1) {
            result = result.substring(0, 1).toUpperCase();
        }
        else if(result.length() >= 2) {
            result = result.substring(0, 1).toUpperCase() + result.substring(1);
        }
        return result;
    }

    public static String skillTitle(String in) {
        return common(in, MIN_SKILL_TITLE_LENGTH, MAX_SKILL_TITLE_LENGTH, true);
    }

    public static String skillDescription(String in) {
        return common(in, MIN_SKILL_DESCRIPTION_LENGTH, MAX_SKILL_DESCRIPTION_LENGTH, true);
    }

    public static String assignmentTitle(String in) {
        return common(in, MIN_ASSIGNMENT_TITLE_LENGTH, MAX_ASSIGNMENT_TITLE_LENGTH, true);
    }

    public static String assignmentDescription(String in) {
        return common(in, MIN_ASSIGNMENT_DESCRIPTION_LENGTH, MAX_ASSIGNMENT_DESCRIPTION_LENGTH, true);
    }

    public static String activityText(String in) {
        return common(in, MIN_ACTIVITY_TEXT_LENGTH, MAX_ACTIVITY_TEXT_LENGTH, false);
    }

}
