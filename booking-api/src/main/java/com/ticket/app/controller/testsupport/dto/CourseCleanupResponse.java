package com.ticket.app.controller.testsupport.dto;

import java.util.Map;

public record CourseCleanupResponse(String runId, Map<String, Integer> deleted, Map<String, Integer> notFound, Map<String, Integer> rejected) {}
