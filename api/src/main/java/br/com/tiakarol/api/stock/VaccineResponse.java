package br.com.tiakarol.api.stock;

import java.util.UUID;

record VaccineResponse(UUID id, String name, String vaccineType, String manufacturer, boolean active) { }
