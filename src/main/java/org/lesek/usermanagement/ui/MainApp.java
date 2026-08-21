package org.lesek.usermanagement.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.lesek.usermanagement.UserManagementApplication;
import org.lesek.usermanagement.policy.condition.ConditionFactory;
import org.lesek.usermanagement.service.OrganizationUnitService;
import org.lesek.usermanagement.service.PolicyAssignmentService;
import org.lesek.usermanagement.service.PolicyEvaluationService;
import org.lesek.usermanagement.service.PolicyService;
import org.lesek.usermanagement.service.UserService;
import org.lesek.usermanagement.ui.controller.WelcomeController;
import org.lesek.usermanagement.ui.navigation.LoadedView;
import org.lesek.usermanagement.ui.navigation.SceneNavigator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Application entry point. The backend beans are wired by Spring
 * ({@link UserManagementApplication}); this class starts the Spring context
 * and the JavaFX shell, and bundles the beans into an {@link AppContext} so
 * FXML controllers can use them.
 * <p>
 * JavaFX instantiates this class itself via reflection, so Spring never
 * manages it - field {@code @Autowired} does nothing here. Beans are fetched
 * from the context explicitly instead.
 */
public class MainApp extends Application {

    private ConfigurableApplicationContext context;
    private UserService userService;
    private PolicyService policyService;
    private PolicyEvaluationService policyEvaluationService;
    private PolicyAssignmentService policyAssignmentService;
    private ConditionFactory conditionFactory;
    private OrganizationUnitService organizationUnitService;

    @Override
    public void init() {
        context = SpringApplication.run(UserManagementApplication.class);
        this.userService = context.getBean(UserService.class);
        this.policyService = context.getBean(PolicyService.class);
        this.policyEvaluationService = context.getBean(PolicyEvaluationService.class);
        this.policyAssignmentService = context.getBean(PolicyAssignmentService.class);
        this.conditionFactory = context.getBean(ConditionFactory.class);
        this.organizationUnitService = context.getBean(OrganizationUnitService.class);
    }

    @Override
    public void start(Stage primaryStage) {
        AppContext appContext = new AppContext(userService, policyService, policyEvaluationService,
                policyAssignmentService, conditionFactory, organizationUnitService);
        SceneNavigator navigator = new SceneNavigator(appContext);

        LoadedView<WelcomeController> welcome = navigator.load("/fxml/welcome-view.fxml");
        navigator.showInitial(welcome);

        Scene scene = new Scene(navigator.getRoot(), 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        primaryStage.setTitle("User & Policy Manager");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Policies.json/users.json may have been hand-edited while the
        // application was not running, so the persisted assignment snapshot
        // could be stale - recompute it once, showing a small spinner for
        // the (usually brief) duration.
        navigator.runBusy(policyAssignmentService::recomputeForAllUsers, null);
    }

    @Override
    public void stop() {
        context.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
