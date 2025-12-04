package com.wang.deerassistant.service;

import com.wang.deerassistant.dto.KbRouteDecision;
import com.wang.deerassistant.entity.KnowledgeBase;

import java.util.List;

public interface KbRoutingService {
    KbRouteDecision route(String question, List<KnowledgeBase> candidates);
}
