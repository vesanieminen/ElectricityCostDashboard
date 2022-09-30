package com.vesanieminen.froniusvisualizer.services.model;

import java.time.LocalDateTime;
import java.util.List;

public class NordpoolResponse {

    public Data data;

    public class Data {
        public boolean IsDivided;
        public LocalDateTime DataStartdate;
        public LocalDateTime DataEnddate;
        public LocalDateTime DateUpdated;
        public List<String> Units;
        public List<Row> Rows;
    }

    public class Row {
        public LocalDateTime StartTime;
        public LocalDateTime EndTime;
        public String Name;
        public List<Column> Columns;
    }

    public class Column {
        public String Name;
        public String Value;
        public int Index;
        public int Scale;
    }

}
