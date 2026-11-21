package com.finantx.recon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AnomalyService {

    private final Resource rulesFile;

    public AnomalyService(@Value("${anomaly.rules-file}") Resource rulesFile) {
        this.rulesFile = rulesFile;
    }

    // TODO: load rules from rulesFile and implement anomaly detection
}
