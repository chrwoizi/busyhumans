package com.c5000.mastery.shared.services;

import com.c5000.mastery.shared.AccessException;
import com.c5000.mastery.shared.data.auth.AuthResultD;
import com.c5000.mastery.shared.data.auth.CredentialCheckResultD;
import com.c5000.mastery.shared.data.auth.EmailSetResult;
import com.c5000.mastery.shared.data.base.*;
import com.c5000.mastery.shared.data.combined.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.ArrayList;
import java.util.Date;

@RemoteServiceRelativePath("rpc")
public interface IMasteryS extends RemoteService {
    public static class Instance {
        private static final IMasterySAsync ourInstance = (IMasterySAsync) GWT.create(IMasteryS.class);

        public static IMasterySAsync get() {
            return ourInstance;
        }
    }

    public InitResultD init(long clientTime);

    @Deprecated
    public ClockSyncResultD syncClocks(long clientTime);

    public SyncResultD sync(long clientTime);

    public void announcementClosed(Date showTime);

    public void createAnnouncement(String text, int showInSeconds, Integer hideInSeconds, boolean isMaintenance) throws AccessException;

    public AuthResultD authWithFacebook(String username, String accessToken);

    public AuthResultD authWithTwitter(String verifier);

    public AuthResultD authWithAnon(String username, String passwordClear);

    public AuthResultD authWithSession();

    public AnonRegisterResultD authWithNewAnon(String recaptchaChallenge, String recaptchaResponse, String username, String passwordClear, String name, String pictureToken);

    public AuthResultD unauth();

    public CredentialCheckResultD checkGoogleCredential() throws AccessException;

    public CredentialCheckResultD checkTwitterCredential() throws AccessException;

    public Boolean setGoogleCredential(String authCode) throws AccessException;

    public PersonD getMyself() throws AccessException;

    public PersonD getPerson(String personId) throws AccessException;

    public ArrayList<AchievementVD> getPersonAchievements(String personId, int offset) throws AccessException;

    public AchievementVD getAchievement(String achievementId) throws AccessException;

    public AssignmentVD getAssignment(String assignmentId) throws AccessException;

    public boolean assignmentExists(String title) throws AccessException;

    public ArrayList<AssignmentVD> getCompletedAssignments(String personId, int offset) throws AccessException;

    public ArrayList<AssignmentVD> getActiveAssignments(int offset, SortBy sortBy) throws AccessException;

    public ArrayList<AssignmentVD> getCreatedAssignments(String personId, int offset) throws AccessException;

    public Integer getNewAssignmentReward() throws AccessException;

    public AssignmentVD createAssignment(AssignmentCreationParamsD params) throws AccessException;

    public boolean deleteAssignment(String assignmentId) throws AccessException;

    public AssignmentVD speedupAssignment(String assignmentId, int days) throws AccessException;

    public ActivityVD createActivity(String assignmentId, java.util.ArrayList<ContentBlockD> contentBlocks) throws AccessException;

    public ActivityDeletedVD deleteActivity(String activityId) throws AccessException;

    public ActivityD rateActivity(String activityId, int rating) throws AccessException;

    public ArrayList<SkillD> suggestExistingSkills(String title) throws AccessException;

    public ArrayList<TokenizedResourceD> suggestSkillPictures(String skillTitle) throws AccessException;

    public ArrayList<VideoD> getYoutubeVideos() throws AccessException;

    public VideoUploadFormD getVideoUploadForm(String assignmentId) throws AccessException;

    public VideoD getVideo(String videoId) throws AccessException;

    public AssignmentVD setAssignmentAbuseReport(String assignmentId, boolean isAbuse) throws AccessException;

    public ActivityD setActivityAbuseReport(String activityId, boolean isAbuse) throws AccessException;

    public SkillD setSkillAbuseReport(String skillId, boolean isAbuse) throws AccessException;

    public ArrayList<AssignmentVD> getAbusedAssignments() throws AccessException;

    public ArrayList<ActivityVD> getAbusedActivities() throws AccessException;

    public ArrayList<SkillD> getAbusedSkills() throws AccessException;

    public AssignmentVD clearAssignmentAbuseReports(String assignmentId) throws AccessException;

    public ActivityD clearActivityAbuseReports(String activityId) throws AccessException;

    public SkillD clearSkillAbuseReports(String skillId) throws AccessException;

    public SearchResultsD search(String searchTerm) throws AccessException;

    public void confirmTos() throws AccessException;

    public SkillD getSkill(String skillId) throws AccessException;

    public boolean deleteSkill(String skillId) throws AccessException;

    public ArrayList<AssignmentVD> getAssignmentsBySkill(String skillId, int offset) throws AccessException;

    public AssignmentVD boostAssignment(String assignmentId, int boost) throws AccessException;

    public RankingsD getRankings() throws AccessException;

    public void setCloak(String accountId) throws AccessException;

    public ArrayList<TagD> getSkillTagCloud() throws AccessException;

    public String createTestPerson(String name, String picture) throws AccessException;

    public ArrayList<PersonAdminInfoD> getPersonAdminInfos() throws AccessException;

    public EmailSetResult setEmailAddress(String email, String oldPasswordClear, String newPasswordClear) throws AccessException;

    public void subscribeToAllAssignments() throws AccessException;

    public void subscribe(String assignmentId) throws AccessException;
    public void unsubscribe(String assignmentId) throws AccessException;

    public PreferencesD getPreferences() throws AccessException;
    public void setPreferences(PreferencesD preferences) throws AccessException;

    public void sendNotifications() throws AccessException;
}
