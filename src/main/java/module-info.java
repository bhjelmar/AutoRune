module AutoRune {
	requires javafx.graphics;
	requires static lombok;
	requires static org.mapstruct.processor;
	requires unirest.java;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.core;
	requires httpclient;
	requires javafx.fxml;
	requires commons.lang3;
	requires gson;
	requires javafx.controls;
	requires java.sql;
	requires javafx.media;
	requires javafx.web;
	requires java.datatransfer;
	requires java.desktop;
	requires jsoup;
	requires com.fasterxml.jackson.annotation;
	requires json;
	requires org.apache.logging.log4j;

	opens com.bhjelmar.ui to javafx.fxml;
	opens com.bhjelmar to javafx.graphics;
	opens com.bhjelmar.api.response to gson;
	opens com.bhjelmar.data to gson;
}
