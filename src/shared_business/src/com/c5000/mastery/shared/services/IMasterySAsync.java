package com.c5000.mastery.shared.services;

import com.c5000.mastery.shared.data.auth.AuthResultD;
import com.c5000.mastery.shared.data.auth.CredentialCheckResultD;
import com.c5000.mastery.shared.data.auth.EmailSetResult;
import com.c5000.mastery.shared.data.base.*;
import com.c5000.mastery.shared.data.combined.*;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;
import java.util.Date;

public interface IMasterySAsync {
    void init(long clientTime, AsyncCallback<InitResultD> asyncCallback);

    @Deprecated
    void syncClocks(long clientTime, AsyncCallback<ClockSyncResultD> async);

    void sync(long clientTime, AsyncCallback<SyncResultD> async);

    void authWithFacebook(String username, String accessToken, AsyncCallback<AuthResultD> asyncCallback);

    void getMyself(AsyncCallback<PersonD> async);

    void getPersonAchievements(String personId, int offset, AsyncCallback<ArrayList<AchievementVD>> async);

    void getAchievement(String achievementId, AsyncCallback<AchievementVD> async);

    void getCompletedAssignments(String personId, int offset, AsyncCallback<ArrayList<AssignmentVD>> async);

    void getActiveAssignments(int offset, SortBy sortBy, AsyncCallback<ArrayList<AssignmentVD>> async);

    void getCreatedAssignments(String personId, int offset, AsyncCallback<ArrayList<AssignmentVD>> async);

    void createAssignment(AssignmentCreationParamsD params, AsyncCallback<AssignmentVD> async);

    void deleteAssignment(String assignmentId, AsyncCallback<Boolean> async);

    void speedupAssignment(String assignmentId, int days, AsyncCallback<AssignmentVD> async);

    void deleteActivity(String activityId, AsyncCallback<ActivityDeletedVD> async);

    void getPerson(String personId, AsyncCallback<PersonD> async);

    void createActivity(String assignmentId, java.util.ArrayList<ContentBlockD> contentBlocks, AsyncCallback<ActivityVD> async);

    void rateActivity(String activityId, int rating, AsyncCallback<ActivityD> async);

    void suggestExistingSkills(String title, AsyncCallback<ArrayList<SkillD>> async);

    void suggestSkillPictures(String skillTitle, AsyncCallback<ArrayList<TokenizedResourceD>> async);

    void checkGoogleCredential(AsyncCallback<CredentialCheckResultD> async);

    void setGoogleCredential(String authCode, AsyncCallback<Boolean> async);

    void getYoutubeVideos(AsyncCallback<ArrayList<VideoD>> async);

    void getVideoUploadForm(String assignmentId, AsyncCallback<VideoUploadFormD> async);

    void getVideo(String videoId, AsyncCallback<VideoD> async);

    void getAssignment(String assignmentId, AsyncCallback<AssignmentVD> async);

    void setAssignmentAbuseReport(String assignmentId, boolean isAbuse, AsyncCallback<AssignmentVD> async);

    void setActivityAbuseReport(String activityId, boolean isAbuse, AsyncCallback<ActivityD> async);

    void getAbusedAssignments(AsyncCallback<ArrayList<AssignmentVD>> async);

    void getAbusedActivities(AsyncCallback<ArrayList<ActivityVD>> async);

    void search(String searchTerm, AsyncCallback<SearchResultsD> async);

    void clearAssignmentAbuseReports(String assignmentId, AsyncCallback<AssignmentVD> async);

    void clearActivityAbuseReports(String activityId, AsyncCallback<ActivityD> async);

    void clearSkillAbuseReports(String skillId, AsyncCallback<SkillD> async);

    void confirmTos(AsyncCallback<Void> async);

    void getSkill(String skillId, AsyncCallback<SkillD> async);

    void deleteSkill(String skillId, AsyncCallback<Boolean> async);

    void getAbusedSkills(AsyncCallback<ArrayList<SkillD>> async);

    void setSkillAbuseReport(String skillId, boolean isAbuse, AsyncCallback<SkillD> async);

    void getAssignmentsBySkill(String skillId, int offset, AsyncCallback<ArrayList<AssignmentVD>> async);

    void getNewAssignmentReward(AsyncCallback<Integer> async);

    void boostAssignment(String assignmentId, int boost, AsyncCallback<AssignmentVD> async);

    void getRankings(AsyncCallback<RankingsD> async);

    void setCloak(String accountId, AsyncCallback<Void> async);

    void assignmentExists(String title, AsyncCallback<Boolean> async);

    void getSkillTagCloud(AsyncCallback<ArrayList<TagD>> async);

    void createTestPerson(String name, String picture, AsyncCallback<String> async);

    void authWithTwitter(String verifier, AsyncCallback<AuthResultD> async);

    void checkTwitterCredential(AsyncCallback<CredentialCheckResultD> async);

    void authWithSession(AsyncCallback<AuthResultD> async);

    void unauth(AsyncCallback<AuthResultD> async);

    void announcementClosed(Date showTime, AsyncCallback<Void> async);

    void createAnnouncement(String text, int showInSeconds, Integer hideInSeconds, boolean isMaintenance, AsyncCallback<Void> async);

    void authWithAnon(String username, String passwordClear, AsyncCallback<AuthResultD> async);

    void authWithNewAnon(String recaptchaChallenge, String recaptchaResponse, String username, String passwordClear, String name, String pictureToken, AsyncCallback<AnonRegisterResultD> async);

    void getPersonAdminInfos(AsyncCallback<ArrayList<PersonAdminInfoD>> async);

    void setEmailAddress(String email, String oldPasswordClear, String newPasswordClear, AsyncCallback<EmailSetResult> async);

    void subscribeToAllAssignments(AsyncCallback<Void> async);

    void subscribe(String assignmentId, AsyncCallback<Void> async);

    void unsubscribe(String assignmentId, AsyncCallback<Void> async);

    void setPreferences(PreferencesD preferences, AsyncCallback<Void> async);

    void getPreferences(AsyncCallback<PreferencesD> async);

    void sendNotifications(AsyncCallback<Void> async);
}
