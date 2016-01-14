package com.vincit.go.task.slack.model;


import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class ConfigTest {

    @Test
    public void testCustomColor() {
        assertThat(configWithColor("CUSTOM", "F00F00").getColor(), is("F00F00"));
    }

    @Test
    public void testNoColor() {
        assertThat(configWithColor("NONE", "F00F00").getColor(), nullValue());
        assertThat(configWithColor("NONE", null).getColor(), nullValue());
    }

    @Test
    public void testGoodColor() {
        assertThat(configWithColor("GOOD", "F00F00").getColor(), is("good"));
        assertThat(configWithColor("GOOD", null).getColor(), is("good"));
    }

    @Test
    public void testWarningColor() {
        assertThat(configWithColor("WARNING", "F00F00").getColor(), is("warning"));
        assertThat(configWithColor("WARNING", null).getColor(), is("warning"));
    }

    @Test
    public void testDangerColor() {
        assertThat(configWithColor("DANGER", "F00F00").getColor(), is("danger"));
        assertThat(configWithColor("DANGER", null).getColor(), is("danger"));
    }

    private Config configWithColor(String colorType, String color) {
        return new Config(null, null, null, null, null, null, null, prop(colorType), prop(color));
    }

    private Property prop(String value) {
        return new Property(value);
    }

}