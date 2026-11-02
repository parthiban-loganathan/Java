import java.util.Scanner;

class GradeRecord {
    private String studentName;
    private Double grade;

    public GradeRecord(String studentName, Double grade) {
        this.studentName = studentName;
        this.grade = grade;
    }

    public void displayRecordInfo() {
        System.out.println("Grade for " + studentName + ": " + grade);
    }
}

class EnrollmentRecord {
    private String courseName;
    private Integer studentId;

    public EnrollmentRecord(String courseName, Integer studentId) {
        this.courseName = courseName;
        this.studentId = studentId;
    }

    public void displayRecordInfo() {
        System.out.println("Student " + studentId + " is enrolled in " + courseName);
    }
}

class RecordHolder<T> {
    private T record;

    public RecordHolder(T record) {
        this.record = record;
    }

    public T getRecord() {
        return record;
    }

    public void displayRecordInfo() {
        if (record instanceof GradeRecord) {
            ((GradeRecord) record).displayRecordInfo();
        } else if (record instanceof EnrollmentRecord) {
            ((EnrollmentRecord) record).displayRecordInfo();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String studentName = sc.next();
        Double grade = sc.nextDouble();
        Integer studentId = sc.nextInt();
        String courseName = sc.next();

        RecordHolder<GradeRecord> gHolder = new RecordHolder<>(
                new GradeRecord(studentName, grade));
        RecordHolder<EnrollmentRecord> eHolder = new RecordHolder<>(
                new EnrollmentRecord(courseName, studentId));

        gHolder.getRecord();
        gHolder.displayRecordInfo();

        eHolder.getRecord();
        eHolder.displayRecordInfo();

        sc.close();
    }
}
