package com.vesanieminen.froniusvisualizer.services.model;

import java.time.LocalDateTime;
import java.util.List;

public class NordpoolResponse implements ResponseValidator {

    public Data data;

    public static class Data {
        public boolean IsDivided;
        public LocalDateTime DataStartdate;
        public LocalDateTime DataEnddate;
        public LocalDateTime DateUpdated;
        public List<String> Units;
        public List<Row> Rows;
    }

    public static class Row {
        public LocalDateTime StartTime;
        public LocalDateTime EndTime;
        public String Name;
        public List<Column> Columns;
    }

    public static class Column {
        public String Name;
        public String Value;
        public int Index;
        public int Scale;
    }

    @Override
    public Object[] getObjects() {
        return new Object[]{this, data, data.Rows, data.Units};
    }

}
