package com.dsp.dirt.parse;

public class BiarcsParser {

    /**
     * Parse one input line into a structured record.
     * TODO: implement according to your dataset format.
     */
    public BiarcsRecord parseLine(String line) {
        // TODO: parse fields, dependencies, tokens, POS, count, etc.
        long count = 1L;
        return new BiarcsRecord(count);
    }
}
