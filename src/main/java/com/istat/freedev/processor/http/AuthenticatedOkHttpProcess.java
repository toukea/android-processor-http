package com.istat.freedev.processor.http;

import okhttp3.Call;
import okhttp3.Response;

public abstract class AuthenticatedOkHttpProcess extends OkHttpProcess {
    public final static int STATE_AUTHENTICATION_FAILURE = 401;

    @Override
    protected void enqueueRequest() {
        if (isAuthenticated()) {
            super.enqueueRequest();
        } else {
            pause();
            dispatchState(STATE_AUTHENTICATION_FAILURE);
        }
    }

    @Override
    protected boolean onHttpResponse(Call call, Response response) {
        if (response.code() == STATE_AUTHENTICATION_FAILURE) {
            // il ya un probleme d'authentification
            dispatchState(STATE_AUTHENTICATION_FAILURE);
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        enqueueRequest();
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
}
