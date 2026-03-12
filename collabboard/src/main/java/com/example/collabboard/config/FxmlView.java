package com.example.collabboard.config;

public enum FxmlView {

    LOGIN {
        @Override
        public String getTitle() {
            return "CollabBoard - Login";
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/LoginView.fxml";
        }
    },
    SIGNUP {
        @Override
        public String getTitle() {
            return "CollabBoard - Sign Up";
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/SignupView.fxml";
        }
    },
    DASHBOARD {
        @Override
        public String getTitle() {
            return "CollabBoard - Dashboard";
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/DashboardView.fxml";
        }
    },
    FORGOT_PASSWORD {
        @Override
        public String getTitle() {
            return "CollabBoard - Forgot Password";
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/ForgotPasswordView.fxml";
        }
    },
    RESET_PASSWORD {
        @Override
        public String getTitle() {
            return "CollabBoard - Reset Password";
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/ResetPasswordView.fxml";
        }
    },

    WHITEBOARD {
        @Override
        public String getTitle() {
            return "CollabBoard - Whiteboard";
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/WhiteboardView.fxml";
        }
    };

    public abstract String getTitle();
    public abstract String getFxmlFile();
}

