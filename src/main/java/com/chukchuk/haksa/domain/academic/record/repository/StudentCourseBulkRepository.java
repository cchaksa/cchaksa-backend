package com.chukchuk.haksa.domain.academic.record.repository;

import java.util.List;

public interface StudentCourseBulkRepository {

    void insertAll(List<StudentCourseBulkRow> rows);
}
