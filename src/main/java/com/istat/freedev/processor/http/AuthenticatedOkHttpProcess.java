package com.istat.freedev.processor.http;

import okhttp3.Call;
import okhttp3.Response;

public abstract class AuthenticatedOkHttpProcess extends OkHttpProcess {
    public final static int STATE_AUTHENTICATION_FAILURE = 401;
    boolean authenticationRequired = true;
    int authenticationFailureCount = 0;

    @Override
    protected void enqueueRequest() {
        if (isAuthenticated() || !isAuthenticationRequired()) {
            super.enqueueRequest();
        } else {
            pause();
            authenticationFailureCount++;
            notifyStateChanged(STATE_AUTHENTICATION_FAILURE);
        }
    }

    @Override
    protected boolean onHttpResponse(Call call, Response response) {
        if (isAuthenticationRequired() && response.code() == STATE_AUTHENTICATION_FAILURE) {
            // il ya un probleme d'authentification
            authenticationFailureCount++;
            notifyStateChanged(STATE_AUTHENTICATION_FAILURE);
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.enqueueRequest();
    }

    void resume(Throwable exception) {
        notifyFailed(exception);
    }

    void resume(Error error) {
        notifyError(error);
    }

    /**
     * Defini si le systeme a tous ce qu'il faut pour estimer que ce process peut se lancer sans rencontrer un 401 (bien que cela puisse se reveler faux.)
     *
     * @return
     */
    protected abstract boolean isAuthenticated();

    protected final void setAuthenticationRequired(boolean required) {
        this.authenticationRequired = required;
    }

    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    public int getAuthenticationFailureCount() {
        return authenticationFailureCount;
    }

    public boolean hasAuthenticationFailure() {
        return authenticationFailureCount > 0;
    }
}
