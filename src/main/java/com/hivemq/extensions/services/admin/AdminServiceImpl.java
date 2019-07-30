package com.hivemq.extensions.services.admin;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.services.admin.AdminService;
import com.hivemq.extension.sdk.api.services.admin.LicenseEdition;
import com.hivemq.extension.sdk.api.services.admin.LicenseInformation;
import com.hivemq.extension.sdk.api.services.admin.LifecycleStage;

import javax.inject.Inject;

/**
 * @author Lukas Brandl
 */
public class AdminServiceImpl implements AdminService {

    @NotNull
    private final ServerInformation serverInformation;

    @NotNull
    private LifecycleStage lifecycleStage = LifecycleStage.STARTING;

    @Inject
    public AdminServiceImpl(@NotNull final ServerInformation serverInformation) {
        this.serverInformation = serverInformation;
    }

    public void hivemqStarted() {
        lifecycleStage = LifecycleStage.STARTED_SUCCESSFULLY;
    }

    @NotNull
    @Override
    public ServerInformation getServerInformation() {
        return serverInformation;
    }

    @NotNull
    @Override
    public LifecycleStage getCurrentStage() {
        return lifecycleStage;
    }

    @NotNull
    @Override
    public LicenseInformation getLicenseInformation() {
        return new LicenseInformationImpl(LicenseEdition.COMMUNITY);
    }

}
