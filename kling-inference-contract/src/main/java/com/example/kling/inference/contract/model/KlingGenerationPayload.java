package com.example.kling.inference.contract.model;

public sealed interface KlingGenerationPayload permits VideoGenerationPayload, ImageGenerationPayload {
}
