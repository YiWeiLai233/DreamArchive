package com.yiweilai.DreamArchive.DTO;

import java.util.List;

public class AI {
    private String model;
    private String temperature;
    private List<messages> messages;


    public AI() {
    }

    public AI(String model, String temperature, List<messages> messages) {
        this.model = model;
        this.temperature = temperature;
        this.messages = messages;
    }

    /**
     * 获取
     * @return model
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置
     * @param model
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取
     * @return temperature
     */
    public String getTemperature() {
        return temperature;
    }

    /**
     * 设置
     * @param temperature
     */
    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    /**
     * 获取
     * @return messages
     *
     */
    public List<messages> getMessages() {
        return messages;
    }

    /**
     * 设置
     * @param messages
     */
    public void setMessages(List<messages> messages) {
        this.messages = messages;
    }

    public String toString() {
        return "AI{model = " + model + ", temperature = " + temperature + ", messages = " + messages + "}";
    }
}
