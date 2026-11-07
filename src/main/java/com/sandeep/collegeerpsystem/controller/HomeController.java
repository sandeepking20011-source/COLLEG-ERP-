package com.sandeep.collegeerpsystem.controller;

import com.sandeep.collegeerpsystem.entity.Fee;
import com.sandeep.collegeerpsystem.entity.Payment;
import com.sandeep.collegeerpsystem.entity.Student;
import com.sandeep.collegeerpsystem.entity.Teacher;
import com.sandeep.collegeerpsystem.repository.FeeRepository;
import com.sandeep.collegeerpsystem.repository.PaymentRepository;
import com.sandeep.collegeerpsystem.repository.StudentRepository;
import com.sandeep.collegeerpsystem.repository.TeacherRepository;
import com.sandeep.collegeerpsystem.repository.TimetableRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class HomeController {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final StudentRepository studentRepository;
    private final FeeRepository feeRepository;
    private final PaymentRepository paymentRepository;
    private final TeacherRepository teacherRepository;
    private final TimetableRepository timetableRepository;
    private final JdbcTemplate jdbcTemplate;
    public HomeController(StudentRepository studentRepository, FeeRepository feeRepository, PaymentRepository paymentRepository, TeacherRepository teacherRepository, TimetableRepository timetableRepository, JdbcTemplate jdbcTemplate) {
        this.studentRepository = studentRepository;
        this.feeRepository = feeRepository;
        this.paymentRepository = paymentRepository;
        this.teacherRepository = teacherRepository;
        this.timetableRepository = timetableRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String home() {
        return "login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login/authenticate")
    @ResponseBody
    public java.util.Map<String,Object> authenticateLogin(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam String password,
            jakarta.servlet.http.HttpSession session) {

        java.util.Map<String,Object> result = new java.util.LinkedHashMap<>();

        if (role == null || role.isBlank() ||
                email == null || email.isBlank() ||
                password == null || password.isBlank()) {
            result.put("success", false);
            result.put("message", "Role, email/username aur password zaroori hain.");
            return result;
        }

        String selectedRole = role.trim().toLowerCase(java.util.Locale.ROOT)
            .replace('_', '-').replace(' ', '-');

        if ("accountant".equals(selectedRole)) {
            selectedRole = "fees-module";
        }

        if (!java.util.List.of("admin", "teacher", "student", "fees-module").contains(selectedRole)) {
            result.put("success", false);
            result.put("message", "Invalid role selected.");
            return result;
        }

        String identifier = email.trim();

                java.util.List<java.util.Map<String,Object>> accounts;

        if ("student".equals(selectedRole)) {
            accounts = jdbcTemplate.queryForList(
                "SELECT u.user_id, u.username, u.password, u.role " +
                "FROM users u INNER JOIN students s ON s.user_id = u.user_id " +
                "WHERE LOWER(s.enrollment_no) = LOWER(?) " +
                "AND COALESCE(u.status, 'ACTIVE') = 'ACTIVE' " +
                "AND u.active = b'1' LIMIT 1",
                identifier
            );
        } else {
            accounts = jdbcTemplate.queryForList(
                "SELECT user_id, username, password, role FROM users " +
                "WHERE (LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?)) " +
                "AND COALESCE(status, 'ACTIVE') = 'ACTIVE' AND active = b'1' LIMIT 1",
                identifier, identifier
            );
        }
if (accounts.isEmpty()) {
            result.put("success", false);
            result.put("message", "Invalid login details or inactive account.");
            return result;
        }

        java.util.Map<String,Object> account = accounts.get(0);

        String storedRole = String.valueOf(account.get("role"))
            .trim().toLowerCase(java.util.Locale.ROOT)
            .replace('_', '-').replace(' ', '-');

        if ("accountant".equals(storedRole)) {
            storedRole = "fees-module";
        }

        String storedPassword = String.valueOf(account.get("password"));
        boolean passwordMatches = false;

        try {
            passwordMatches = passwordEncoder.matches(password, storedPassword);
        } catch (IllegalArgumentException ignored) {
            passwordMatches = false;
        }

        if (!selectedRole.equals(storedRole) || !passwordMatches) {
            result.put("success", false);
            result.put("message", "Invalid login details or inactive account.");
            return result;
        }

        session.setAttribute("loginUserId", account.get("user_id"));
        session.setAttribute("loginUsername", account.get("username"));
        session.setAttribute("loginRole", storedRole);

        String redirectUrl;
        switch (selectedRole) {
            case "admin":
                redirectUrl = "/admin/dashboard";
                break;
            case "teacher":
                redirectUrl = "/teacher/dashboard";
                break;
            case "student":
                redirectUrl = "/student/dashboard";
                break;
            default:
                redirectUrl = "/accountant/dashboard";
                break;
        }

        result.put("success", true);
        result.put("message", "Login verified.");
        result.put("redirectUrl", redirectUrl);
        return result;
    }
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/teacher/dashboard")
    public String teacherDashboard() {
        return "teacher-dashboard";
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(
            Model model,
            jakarta.servlet.http.HttpSession session) {

        Object rawUserId = session.getAttribute("loginUserId");
        Object rawRole = session.getAttribute("loginRole");

        if (!(rawUserId instanceof Number) ||
                rawRole == null ||
                !"student".equalsIgnoreCase(rawRole.toString())) {
            return "redirect:/login";
        }

        Integer userId = ((Number) rawUserId).intValue();

        Student student = studentRepository.findByUserId(userId).orElse(null);

        if (student == null) {
            model.addAttribute("studentNotLinked", true);
            return "student-dashboard";
        }

        model.addAttribute("student", student);
        model.addAttribute("fees", feeRepository.findByStudentId(student.getStudentId()));
        return "student-dashboard";
    }

    @GetMapping("/accountant/dashboard")
    public String accountantDashboard() {
        return "fees-module";
    }

    @GetMapping({"/admin/students", "/admin/student.html"})
    public String studentsPage(
            @RequestParam(required = false) String edit,
            Model model) {

        if (!model.containsAttribute("student")) {
            model.addAttribute("student", new Student());
        }

        model.addAttribute("students", studentRepository.findAll());
        return "students";
    }

    @PostMapping("/admin/students/save")
    public String saveStudent(
            @ModelAttribute("student") Student student,
            RedirectAttributes redirectAttributes) {

        if (student.getEnrollmentNo() == null
                || student.getEnrollmentNo().isBlank()
                || student.getFirstName() == null
                || student.getFirstName().isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "error", "Enrollment No. aur First Name bharna zaroori hai.");
            return "redirect:/admin/students";
        }

        student.setEnrollmentNo(student.getEnrollmentNo().trim());
        student.setFirstName(student.getFirstName().trim());

        try {
            studentRepository.save(student);
            redirectAttributes.addFlashAttribute(
                    "message", "Student record successfully save ho gaya.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "error", "Enrollment No. duplicate hai ya database constraint fail hua.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error", "Student save nahi hua. Database aur entered values check karein.");
        }

        return "redirect:/admin/students";
    }

    @GetMapping("/admin/students/edit/{id}")
    public String editStudent(
            @PathVariable Integer id,
            Model model,
            RedirectAttributes redirectAttributes) {

        Student student = studentRepository.findById(id).orElse(null);

        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Student record nahi mila.");
            return "redirect:/admin/students";
        }

        model.addAttribute("student", student);
        model.addAttribute("students", studentRepository.findAll());
        return "students";
    }

    @GetMapping("/admin/students/delete/{id}")
    public String deleteStudent(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            if (!studentRepository.existsById(id)) {
                redirectAttributes.addFlashAttribute("error", "Student record nahi mila.");
            } else {
                studentRepository.deleteById(id);
                redirectAttributes.addFlashAttribute("message", "Student record delete ho gaya.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error", "Record delete nahi hua. Ho sakta hai doosri table is student ko use kar rahi ho.");
        }

        return "redirect:/admin/students";
    }

    @GetMapping("/admin/teacher.html")
    public String legacyAdminTeacherPage() {
        return "redirect:/admin/teachers";
    }

    @GetMapping("/admin/teachers")
   public String teachersPage(Model model) {
       model.addAttribute("teachers", jdbcTemplate.queryForList(
           "SELECT t.teacher_id, t.employee_code, t.first_name, t.last_name, " +
           "COALESCE(d.department_name, 'Not assigned') AS department_name, " +
           "COALESCE(u.email, 'Not linked') AS email, t.phone, t.qualification, t.joining_date " +
           "FROM teachers t " +
           "LEFT JOIN departments d ON d.department_id = t.department_id " +
           "LEFT JOIN users u ON u.user_id = t.user_id " +
           "ORDER BY t.teacher_id DESC"
       ));

       model.addAttribute("departments", jdbcTemplate.queryForList(
           "SELECT department_id, department_name FROM departments ORDER BY department_name"
       ));

       return "teachers";
   }

    @PostMapping("/admin/teachers/save")
    @Transactional(rollbackFor = Exception.class)
    public String saveTeacher(
            @RequestParam String employeeCode,
            @RequestParam String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestParam(required = false, defaultValue = "0") Integer departmentId,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String qualification,
            @RequestParam(required = false, defaultValue = "") String joiningDate,
            @RequestParam String email,
            @RequestParam String initialPassword,
            RedirectAttributes redirectAttributes) {

        String code = employeeCode == null ? "" : employeeCode.trim();
        String first = firstName == null ? "" : firstName.trim();
        String mail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        String rawPassword = initialPassword == null ? "" : initialPassword;

        if (code.isBlank() || first.isBlank() || mail.isBlank() || rawPassword.isBlank()) {
            redirectAttributes.addFlashAttribute(
                "error", "Employee Code, First Name, Email aur Initial Password zaroori hain."
            );
            return "redirect:/admin/teachers";
        }

        if (!mail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            redirectAttributes.addFlashAttribute("error", "Valid email address bharein.");
            return "redirect:/admin/teachers";
        }

        if (rawPassword.length() < 8) {
            redirectAttributes.addFlashAttribute(
                "error", "Initial Password kam se kam 8 characters ka hona chahiye."
            );
            return "redirect:/admin/teachers";
        }

        try {
            Integer duplicateCode = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM teachers WHERE employee_code = ?",
                Integer.class, code
            );

            if (duplicateCode != null && duplicateCode > 0) {
                redirectAttributes.addFlashAttribute("error", "Employee Code pehle se registered hai.");
                return "redirect:/admin/teachers";
            }

            Integer duplicateAccount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?)",
                Integer.class, mail, code
            );

            if (duplicateAccount != null && duplicateAccount > 0) {
                redirectAttributes.addFlashAttribute("error", "Email ya username pehle se registered hai.");
                return "redirect:/admin/teachers";
            }

            jdbcTemplate.update(
                "INSERT INTO users (username, email, password, role, status, active) " +
                "VALUES (?, ?, ?, 'teacher', 'ACTIVE', b'1')",
                code, mail, passwordEncoder.encode(rawPassword)
            );

            Integer userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE username = ?",
                Integer.class, code
            );

            jdbcTemplate.update(
                "INSERT INTO teachers " +
                "(user_id, employee_code, first_name, last_name, department_id, phone, qualification, joining_date) " +
                "VALUES (?, ?, ?, NULLIF(?, ''), NULLIF(?, 0), NULLIF(?, ''), NULLIF(?, ''), " +
                "STR_TO_DATE(NULLIF(?, ''), '%Y-%m-%d'))",
                userId,
                code,
                first,
                lastName == null ? "" : lastName.trim(),
                departmentId == null ? 0 : departmentId,
                phone == null ? "" : phone.trim(),
                qualification == null ? "" : qualification.trim(),
                joiningDate == null ? "" : joiningDate.trim()
            );

            redirectAttributes.addFlashAttribute(
                "message", "Teacher aur login account successfully save ho gaye."
            );
        } catch (Exception ex) {
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus().setRollbackOnly();

            redirectAttributes.addFlashAttribute(
                "error", "Teacher account save nahi hua. Duplicate email/code ya database constraint check karein."
            );
        }

        return "redirect:/admin/teachers";
    }

    @GetMapping("/admin/teachers/edit/{id}")
    public String editTeacher(
            @PathVariable("id") int id,
            Model model,
            RedirectAttributes redirectAttributes) {

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM teachers WHERE teacher_id = ?",
            Integer.class,
            id
        );

        if (count == null || count == 0) {
            redirectAttributes.addFlashAttribute(
                "error", "Teacher record nahi mila."
            );
            return "redirect:/admin/teachers";
        }

        model.addAttribute("teacher", jdbcTemplate.queryForMap(
            "SELECT t.teacher_id, t.user_id, t.employee_code, t.first_name, t.last_name, " +
            "t.department_id, t.phone, t.qualification, t.joining_date, u.email " +
            "FROM teachers t LEFT JOIN users u ON u.user_id = t.user_id " +
            "WHERE t.teacher_id = ?",
            id
        ));

        model.addAttribute("departments", jdbcTemplate.queryForList(
            "SELECT department_id, department_name " +
            "FROM departments ORDER BY department_name"
        ));

        return "teacher-edit";
    }

    @PostMapping("/admin/teachers/update/{id}")
    @Transactional(rollbackFor = Exception.class)
    public String updateTeacher(
            @PathVariable("id") int id,
            @RequestParam String employeeCode,
            @RequestParam String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestParam(required = false, defaultValue = "0") Integer departmentId,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String qualification,
            @RequestParam(required = false, defaultValue = "") String joiningDate,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String initialPassword,
            RedirectAttributes redirectAttributes) {

        String code = employeeCode == null ? "" : employeeCode.trim();
        String first = firstName == null ? "" : firstName.trim();
        String mail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        String rawPassword = initialPassword == null ? "" : initialPassword;

        if (code.isBlank() || first.isBlank() || mail.isBlank()) {
            redirectAttributes.addFlashAttribute(
                "error", "Employee Code, First Name aur Email zaroori hain."
            );
            return "redirect:/admin/teachers";
        }

        if (!mail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            redirectAttributes.addFlashAttribute("error", "Valid email address bharein.");
            return "redirect:/admin/teachers";
        }

        if (!rawPassword.isEmpty() && rawPassword.length() < 8) {
            redirectAttributes.addFlashAttribute(
                "error", "Naya Password kam se kam 8 characters ka hona chahiye."
            );
            return "redirect:/admin/teachers";
        }

        try {
            java.util.List<java.util.Map<String,Object>> currentRows =
                jdbcTemplate.queryForList(
                    "SELECT user_id FROM teachers WHERE teacher_id = ?", id
                );

            if (currentRows.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Teacher record nahi mila.");
                return "redirect:/admin/teachers";
            }

            Object userIdValue = currentRows.get(0).get("user_id");
            Integer userId = userIdValue instanceof Number
                ? ((Number) userIdValue).intValue() : null;

            if (userId == null && rawPassword.isBlank()) {
                redirectAttributes.addFlashAttribute(
                    "error", "Login account banane ke liye kam se kam 8-character Initial Password bharein."
                );
                return "redirect:/admin/teachers";
            }

            Integer duplicateCode = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM teachers WHERE employee_code = ? AND teacher_id <> ?",
                Integer.class, code, id
            );

            if (duplicateCode != null && duplicateCode > 0) {
                redirectAttributes.addFlashAttribute(
                    "error", "Employee Code kisi doosre Teacher ke saath registered hai."
                );
                return "redirect:/admin/teachers";
            }

            Integer excludedUserId = userId == null ? -1 : userId;

            Integer duplicateAccount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users " +
                "WHERE (LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?)) " +
                "AND user_id <> ?",
                Integer.class, mail, code, excludedUserId
            );

            if (duplicateAccount != null && duplicateAccount > 0) {
                redirectAttributes.addFlashAttribute(
                    "error", "Email ya username kisi doosre account mein pehle se maujood hai."
                );
                return "redirect:/admin/teachers";
            }

            if (userId == null) {
                jdbcTemplate.update(
                    "INSERT INTO users (username, email, password, role, status, active) " +
                    "VALUES (?, ?, ?, 'teacher', 'ACTIVE', b'1')",
                    code, mail, passwordEncoder.encode(rawPassword)
                );

                userId = jdbcTemplate.queryForObject(
                    "SELECT user_id FROM users WHERE username = ?",
                    Integer.class, code
                );
            } else if (!rawPassword.isEmpty()) {
                jdbcTemplate.update(
                    "UPDATE users SET username = ?, email = ?, password = ?, role = 'teacher' " +
                    "WHERE user_id = ?",
                    code, mail, passwordEncoder.encode(rawPassword), userId
                );
            } else {
                jdbcTemplate.update(
                    "UPDATE users SET username = ?, email = ?, role = 'teacher' WHERE user_id = ?",
                    code, mail, userId
                );
            }

            jdbcTemplate.update(
                "UPDATE teachers SET user_id = ?, employee_code = ?, first_name = ?, " +
                "last_name = NULLIF(?, ''), department_id = NULLIF(?, 0), " +
                "phone = NULLIF(?, ''), qualification = NULLIF(?, ''), " +
                "joining_date = STR_TO_DATE(NULLIF(?, ''), '%Y-%m-%d') " +
                "WHERE teacher_id = ?",
                userId,
                code,
                first,
                lastName == null ? "" : lastName.trim(),
                departmentId == null ? 0 : departmentId,
                phone == null ? "" : phone.trim(),
                qualification == null ? "" : qualification.trim(),
                joiningDate == null ? "" : joiningDate.trim(),
                id
            );

            redirectAttributes.addFlashAttribute(
                "message", "Teacher profile aur login account successfully update ho gaye."
            );
        } catch (Exception ex) {
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus().setRollbackOnly();

            redirectAttributes.addFlashAttribute(
                "error", "Update/account linking fail hua. Duplicate email/code ya database constraint check karein."
            );
        }

        return "redirect:/admin/teachers";
    }

    @PostMapping("/admin/teachers/delete/{id}")
    public String deleteTeacher(
            @PathVariable("id") int id,
            RedirectAttributes redirectAttributes) {

        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM teachers WHERE teacher_id = ?",
                id
            );

            if (deleted > 0) {
                redirectAttributes.addFlashAttribute(
                    "message", "Teacher record delete ho gaya."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                    "error", "Teacher record nahi mila."
                );
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                "error",
                "Teacher delete nahi hua. Ho sakta hai doosri table is record ko reference kar rahi ho."
            );
        }

        return "redirect:/admin/teachers";
    }
    @GetMapping("/admin/courses")
    public String coursesPage(Model model) {
        model.addAttribute("courses", jdbcTemplate.queryForList(
            "SELECT c.course_id, c.course_name, c.course_code, " +
            "c.duration_years, COALESCE(d.department_name, 'Not assigned') AS department_name " +
            "FROM courses c LEFT JOIN departments d ON c.department_id = d.department_id " +
            "ORDER BY c.course_name"
        ));

        model.addAttribute("departments", jdbcTemplate.queryForList(
            "SELECT department_id, department_name FROM departments ORDER BY department_name"
        ));

        model.addAttribute("assignmentTeachers", jdbcTemplate.queryForList(
            "SELECT teacher_id, employee_code, first_name, last_name " +
            "FROM teachers ORDER BY first_name, last_name"
        ));

        model.addAttribute("timetableAssignments", jdbcTemplate.queryForList(
            "SELECT tt.timetable_id, c.course_name, c.course_code, " +
            "CONCAT(t.first_name, ' ', COALESCE(t.last_name, '')) AS teacher_name, " +
            "tt.day_of_week, tt.start_time, tt.end_time, tt.semester, tt.academic_year " +
            "FROM timetable tt " +
            "LEFT JOIN courses c ON c.course_id = tt.course_id " +
            "LEFT JOIN teachers t ON t.teacher_id = tt.teacher_id " +
            "ORDER BY tt.timetable_id DESC"
        ));

        return "courses";
    }

    @PostMapping("/admin/courses/save")
    public String saveCourse(
            @RequestParam String courseName,
            @RequestParam String courseCode,
            @RequestParam Integer durationYears,
            @RequestParam(required = false) Integer departmentId,
            RedirectAttributes redirectAttributes) {

        String name = courseName == null ? "" : courseName.trim();
        String code = courseCode == null ? "" : courseCode.trim();

        if (name.isBlank() || name.length() > 100 ||
                code.isBlank() || code.length() > 30 ||
                durationYears == null || durationYears < 1 || durationYears > 10) {
            redirectAttributes.addFlashAttribute(
                "error", "Course name, code aur valid duration (1-10 years) bharein."
            );
            return "redirect:/admin/courses";
        }

        try {
            if (departmentId != null && departmentId > 0) {
                Integer departmentCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM departments WHERE department_id = ?",
                    Integer.class, departmentId
                );
                if (departmentCount == null || departmentCount == 0) {
                    redirectAttributes.addFlashAttribute(
                        "error", "Selected department database mein nahi mila."
                    );
                    return "redirect:/admin/courses";
                }
            }

            jdbcTemplate.update(
                "INSERT INTO courses (course_name, course_code, duration_years, department_id) " +
                "VALUES (?, ?, ?, NULLIF(?, 0))",
                name, code, durationYears,
                departmentId == null ? 0 : departmentId
            );

            redirectAttributes.addFlashAttribute(
                "message", "Course successfully database mein save ho gaya."
            );
        } catch (org.springframework.dao.DataAccessException ex) {
            redirectAttributes.addFlashAttribute(
                "error", "Course save nahi hua. Course code duplicate ho sakta hai; database constraints check karein."
            );
        }

        return "redirect:/admin/courses";
    }

    @GetMapping("/admin/departments")
    public String departmentsPage(Model model) {
        model.addAttribute("departments", jdbcTemplate.queryForList(
            "SELECT department_id, department_name, department_code FROM departments ORDER BY department_name"
        ));
        return "departments";
    }

    @PostMapping("/admin/departments/save")
    public String saveDepartment(
            @RequestParam String departmentName,
            @RequestParam String departmentCode,
            RedirectAttributes redirectAttributes) {
        String name = departmentName == null ? "" : departmentName.trim();
        String code = departmentCode == null ? "" : departmentCode.trim();

        if (name.isBlank() || code.isBlank() ||
                name.length() > 100 || code.length() > 20) {
            redirectAttributes.addFlashAttribute("error",
                "Department name aur code bharna zaroori hai.");
            return "redirect:/admin/departments";
        }

        try {
            jdbcTemplate.update(
                "INSERT INTO departments (department_name, department_code) VALUES (?, ?)",
                name, code
            );
            redirectAttributes.addFlashAttribute("message",
                "Department database mein save ho gaya.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                "Department save nahi hua. Duplicate code ya database constraint check karein.");
        }

        return "redirect:/admin/departments";
    }


    @GetMapping("/teacher/profile")
    public String teacherProfile(Model model, jakarta.servlet.http.HttpSession session) {
        Object rawUserId = session.getAttribute("loginUserId");
        Integer userId = null;

        if (rawUserId instanceof Number) {
            userId = ((Number) rawUserId).intValue();
        } else if (rawUserId != null) {
            try {
                userId = Integer.valueOf(rawUserId.toString());
            } catch (NumberFormatException ignored) {
                // Invalid session user ID
            }
        }

        if (userId != null) {
            Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);

            if (teacher != null) {
                model.addAttribute("teacher", teacher);

                if (teacher.getDepartmentId() != null) {
                    java.util.List<String> departments = jdbcTemplate.query(
                        "SELECT department_name FROM departments WHERE department_id = ?",
                        (rs, rowNum) -> rs.getString("department_name"),
                        teacher.getDepartmentId()
                    );

                    if (!departments.isEmpty()) {
                        model.addAttribute("departmentName", departments.get(0));
                    }
                }
            }
        }

        return "teacher-profile";
    }
    @GetMapping("/teacher/attendance")
public String teacherAttendance(Model model) {
    model.addAttribute("students", studentRepository.findAll());
    return "teacher-attendance";
}

    @GetMapping("/teacher/marks")
    public String teacherMarks(Model model) {
        model.addAttribute("students", jdbcTemplate.queryForList(
            "SELECT student_id, enrollment_no, first_name, last_name FROM students ORDER BY first_name, last_name"
        ));

        model.addAttribute("subjects", jdbcTemplate.queryForList(
            "SELECT subject_id, subject_code, subject_name FROM subjects ORDER BY subject_name"
        ));

        model.addAttribute("exams", jdbcTemplate.queryForList(
            "SELECT exam_id, exam_name, exam_type, exam_date, " +
            "CONCAT(exam_name, ' (', COALESCE(exam_type, 'Exam'), ') - ', " +
            "COALESCE(DATE_FORMAT(exam_date, '%d-%m-%Y'), 'Date not set')) AS exam_label " +
            "FROM exams WHERE COALESCE(LOWER(status), '') <> 'cancelled' " +
            "ORDER BY exam_date DESC, exam_id DESC"
        ));

        model.addAttribute("marksEntries", jdbcTemplate.queryForList(
            "SELECT m.mark_id, m.student_id, m.subject_id, " +
            "CONCAT(s.first_name, ' ', s.last_name) AS student_name, " +
            "s.enrollment_no, sub.subject_name, m.exam_type, " +
            "COALESCE(e.exam_name, CONCAT('Unlinked - ', m.exam_type)) AS exam_display, " +
            "m.marks_obtained, m.max_marks, m.grade, m.exam_date " +
            "FROM marks m " +
            "JOIN students s ON s.student_id = m.student_id " +
            "JOIN subjects sub ON sub.subject_id = m.subject_id " +
            "LEFT JOIN exams e ON e.exam_id = m.exam_id " +
            "ORDER BY m.mark_id DESC"
        ));

        return "teacher-marks";
    }

    @PostMapping("/teacher/marks/save")
    public String saveTeacherMarks(
            @RequestParam String studentId,
            @RequestParam String subjectId,
            @RequestParam Long examId,
            @RequestParam String marksObtained,
            @RequestParam String maxMarks,
            RedirectAttributes redirectAttributes) {
        try {
            int sid = Integer.parseInt(studentId);
            int subid = Integer.parseInt(subjectId);

            if (examId == null || examId <= 0) {
                throw new IllegalArgumentException("Invalid examination.");
            }

            java.math.BigDecimal obtained = new java.math.BigDecimal(marksObtained);
            java.math.BigDecimal maximum = new java.math.BigDecimal(maxMarks);

            if (obtained.signum() < 0 || maximum.signum() <= 0 ||
                    obtained.compareTo(maximum) > 0 ||
                    obtained.scale() > 2 || maximum.scale() > 2 ||
                    obtained.precision() > 5 || maximum.precision() > 5) {
                throw new IllegalArgumentException("Invalid marks details.");
            }

            if (jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM students WHERE student_id = ?", Integer.class, sid) == 0 ||
                jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM subjects WHERE subject_id = ?", Integer.class, subid) == 0) {
                throw new IllegalArgumentException("Student ya subject nahi mila.");
            }

            java.util.Map<String, Object> exam = jdbcTemplate.queryForMap(
                "SELECT exam_type, exam_date, status FROM exams WHERE exam_id = ?",
                examId
            );

            String status = exam.get("status") == null
                    ? "" : String.valueOf(exam.get("status"));

            if ("cancelled".equalsIgnoreCase(status)) {
                throw new IllegalArgumentException("Cancelled examination.");
            }

            String type = exam.get("exam_type") == null
                    ? "Exam" : String.valueOf(exam.get("exam_type"));

            if (type.isBlank() || type.length() > 50) {
                throw new IllegalArgumentException("Invalid examination type.");
            }

            Object rawDate = exam.get("exam_date");
            java.sql.Date dateValue = rawDate == null
                    ? null
                    : (rawDate instanceof java.sql.Date)
                        ? (java.sql.Date) rawDate
                        : java.sql.Date.valueOf(rawDate.toString());

            java.math.BigDecimal percentage = obtained.multiply(
                    new java.math.BigDecimal("100")
            ).divide(maximum, 2, java.math.RoundingMode.HALF_UP);

            String grade;
            if (percentage.compareTo(new java.math.BigDecimal("90")) >= 0) grade = "A+";
            else if (percentage.compareTo(new java.math.BigDecimal("80")) >= 0) grade = "A";
            else if (percentage.compareTo(new java.math.BigDecimal("70")) >= 0) grade = "B";
            else if (percentage.compareTo(new java.math.BigDecimal("60")) >= 0) grade = "C";
            else if (percentage.compareTo(new java.math.BigDecimal("40")) >= 0) grade = "D";
            else grade = "F";

            jdbcTemplate.update(
                "INSERT INTO marks (exam_id, student_id, subject_id, exam_type, marks_obtained, max_marks, grade, exam_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                examId, sid, subid, type, obtained, maximum, grade, dateValue
            );

            redirectAttributes.addFlashAttribute(
                "message", "Marks examination ke saath save ho gaye. Grade: " + grade
            );

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                "error", "Marks save nahi hue. Student, subject, examination aur marks details check karein."
            );
        }

        return "redirect:/teacher/marks";
    }
    @GetMapping("/teacher/students")
    public String teacherStudents(Model model) {
        model.addAttribute("students", studentRepository.findAll());
        return "teacher-students";
    }

    // COURSE_ASSIGNMENT_POST_MARKER
    @PostMapping("/admin/timetable/save")
    public String saveCourseAssignment(
            @RequestParam Integer courseId,
            @RequestParam Integer teacherId,
            @RequestParam String dayOfWeek,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String academicYear,
            RedirectAttributes redirectAttributes) {

        String day = dayOfWeek == null ? "" : dayOfWeek.trim();
        String year = academicYear == null ? "" : academicYear.trim();

        if (courseId == null || teacherId == null ||
                !java.util.Arrays.asList(
                    "Monday", "Tuesday", "Wednesday", "Thursday",
                    "Friday", "Saturday", "Sunday"
                ).contains(day) ||
                semester == null || semester < 1 || semester > 12 ||
                year.isBlank() || year.length() > 20) {
            redirectAttributes.addFlashAttribute(
                "error", "Course, teacher, valid day, semester aur academic year bharein."
            );
            return "redirect:/admin/courses";
        }

        try {
            java.time.LocalTime start = java.time.LocalTime.parse(startTime);
            java.time.LocalTime end = java.time.LocalTime.parse(endTime);

            if (!end.isAfter(start)) {
                redirectAttributes.addFlashAttribute(
                    "error", "End time, start time ke baad hona chahiye."
                );
                return "redirect:/admin/courses";
            }

            jdbcTemplate.update(
                "INSERT INTO timetable " +
                "(course_id, teacher_id, day_of_week, start_time, end_time, semester, academic_year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                courseId,
                teacherId,
                day,
                java.sql.Time.valueOf(start),
                java.sql.Time.valueOf(end),
                semester,
                year
            );

            redirectAttributes.addFlashAttribute(
                "message", "Course teacher ko successfully assign ho gaya."
            );
        } catch (java.time.format.DateTimeParseException ex) {
            redirectAttributes.addFlashAttribute(
                "error", "Start aur end time sahi format mein select karein."
            );
        } catch (org.springframework.dao.DataAccessException ex) {
            redirectAttributes.addFlashAttribute(
                "error", "Assignment save nahi hua. Course, teacher aur database constraints check karein."
            );
        }

        return "redirect:/admin/courses";
    }

    @GetMapping("/teacher/courses")
    public String teacherCourses(Model model, jakarta.servlet.http.HttpSession session) {
        Object rawUserId = session.getAttribute("loginUserId");
        Integer userId = null;

        if (rawUserId instanceof Number) {
            userId = ((Number) rawUserId).intValue();
        } else if (rawUserId != null) {
            try {
                userId = Integer.valueOf(rawUserId.toString());
            } catch (NumberFormatException ignored) {
                // Invalid session user ID
            }
        }

        java.util.List<java.util.Map<String, Object>> courses =
            new java.util.ArrayList<>();

        if (userId != null) {
            java.util.List<java.util.Map<String, Object>> courseRows =
                jdbcTemplate.queryForList(
                    "SELECT DISTINCT c.course_id, c.course_name, c.course_code, " +
                    "c.duration_years, " +
                    "COALESCE(d.department_name, 'Not assigned') AS department_name, " +
                    "(SELECT COUNT(*) FROM enrollments e " +
                    " WHERE e.course_id = c.course_id " +
                    " AND UPPER(COALESCE(e.status, 'ACTIVE')) = 'ACTIVE') AS enrolled_students, " +
                    "(SELECT GROUP_CONCAT(DISTINCT tt2.academic_year " +
                    " ORDER BY tt2.academic_year SEPARATOR ', ') " +
                    " FROM timetable tt2 " +
                    " WHERE tt2.teacher_id = t.teacher_id " +
                    " AND tt2.course_id = c.course_id) AS academic_years, " +
                    "(SELECT GROUP_CONCAT(DISTINCT tt3.semester " +
                    " ORDER BY tt3.semester SEPARATOR ', ') " +
                    " FROM timetable tt3 " +
                    " WHERE tt3.teacher_id = t.teacher_id " +
                    " AND tt3.course_id = c.course_id) AS semesters " +
                    "FROM teachers t " +
                    "JOIN timetable tt ON tt.teacher_id = t.teacher_id " +
                    "JOIN courses c ON c.course_id = tt.course_id " +
                    "LEFT JOIN departments d ON d.department_id = c.department_id " +
                    "WHERE t.user_id = ? ORDER BY c.course_name",
                    userId
                );

            for (java.util.Map<String, Object> row : courseRows) {
                java.util.Map<String, Object> course =
                    new java.util.LinkedHashMap<>(row);

                Integer courseId =
                    ((Number) row.get("course_id")).intValue();

                java.util.List<java.util.Map<String, Object>> subjects =
                    jdbcTemplate.queryForList(
                        "SELECT DISTINCT s.subject_name, s.subject_code, " +
                        "s.credits, s.semester " +
                        "FROM timetable tt " +
                        "JOIN subjects s ON s.subject_id = tt.subject_id " +
                        "WHERE tt.teacher_id = ? AND tt.course_id = ? " +
                        "ORDER BY s.semester, s.subject_name",
                        userId, courseId
                    );

                java.util.List<java.util.Map<String, Object>> schedule =
                    jdbcTemplate.queryForList(
                        "SELECT tt.day_of_week, tt.start_time, tt.end_time, " +
                        "tt.semester, tt.academic_year, " +
                        "COALESCE(cl.room_number, 'Not assigned') AS room_number, " +
                        "COALESCE(cl.building_name, '') AS building_name " +
                        "FROM timetable tt " +
                        "LEFT JOIN classrooms cl ON cl.classroom_id = tt.classroom_id " +
                        "WHERE tt.teacher_id = ? AND tt.course_id = ? " +
                        "ORDER BY CASE LOWER(tt.day_of_week) " +
                        "WHEN 'monday' THEN 1 WHEN 'tuesday' THEN 2 " +
                        "WHEN 'wednesday' THEN 3 WHEN 'thursday' THEN 4 " +
                        "WHEN 'friday' THEN 5 WHEN 'saturday' THEN 6 " +
                        "WHEN 'sunday' THEN 7 ELSE 8 END, tt.start_time",
                        userId, courseId
                    );

                course.put("subjects", subjects);
                course.put("schedule", schedule);
                courses.add(course);
            }
        }

        model.addAttribute("courses", courses);
        return "teacher-courses";
    }
    @GetMapping("/teacher/timetable")
    public String teacherTimetable(Model model) {
        model.addAttribute("timetableEntries", timetableRepository.findAll());
        return "teacher-timetable";
    }

    @GetMapping("/teacher/notices")
public String teacherNotices(Model model) {
    model.addAttribute("notices", jdbcTemplate.queryForList(
        "SELECT n.notice_id, n.title, n.description, n.notice_date, n.created_at, " +
        "COALESCE(u.username, 'College Administration') AS posted_by_name " +
        "FROM notices n " +
        "LEFT JOIN users u ON u.user_id = n.posted_by " +
        "ORDER BY n.notice_date DESC, n.notice_id DESC"
    ));
    return "teacher-notices";
}
    @GetMapping("/accountant/students")
    public String feeStudents(Model model) {
        model.addAttribute("fees", feeRepository.findAll());
        return "fees-students";
    }

    @GetMapping("/accountant/pending")
    public String pendingFees(Model model) {
        model.addAttribute("fees", feeRepository.findAll().stream()
            .filter(fee -> fee.getBalance().compareTo(java.math.BigDecimal.ZERO) > 0)
            .toList());
        return "fees-pending";
    }

    @GetMapping("/accountant/fees/new")
    public String newFee(Model model) {
        model.addAttribute("fee", new Fee());
        model.addAttribute("students", studentRepository.findAll());
        return "fees-add";
    }

    @PostMapping("/accountant/fees/save")
    public String saveFee(@ModelAttribute("fee") Fee fee,
                          RedirectAttributes redirectAttributes) {
        Integer studentId = fee.getStudentId();
        if (studentId == null
                || !studentRepository.existsById(studentId)
                || fee.getFeeType() == null || fee.getFeeType().isBlank()
                || fee.getAmount() == null
                || fee.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error",
                    "Valid student, fee type aur positive amount enter karein.");
            return "redirect:/accountant/fees/new";
        }

        fee.setFeeType(fee.getFeeType().trim());
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setPaymentStatus("PENDING");
        fee.setPaymentDate(null);
        fee.setTransactionId(null);
        feeRepository.save(fee);

        redirectAttributes.addFlashAttribute("message",
                "Nayi fee successfully add ho gayi.");
        return "redirect:/accountant/students";
    }

    @PostMapping("/accountant/payments/save")
    @Transactional
    public String recordPayment(
            @RequestParam Integer feeId,
            @RequestParam BigDecimal paymentAmount,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String transactionId,
            RedirectAttributes redirectAttributes) {

        Fee fee = feeRepository.findById(feeId).orElse(null);
        if (fee == null) {
            redirectAttributes.addFlashAttribute("error", "Fee record nahi mila.");
            return "redirect:/accountant/pending";
        }

        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0
                || paymentAmount.compareTo(fee.getBalance()) > 0) {
            redirectAttributes.addFlashAttribute("error",
                    "Payment positive hona chahiye aur remaining balance se zyada nahi ho sakta.");
            return "redirect:/accountant/pending";
        }

        String cleanTransactionId = transactionId == null ? null : transactionId.trim();
        if (cleanTransactionId != null && cleanTransactionId.isBlank()) {
            cleanTransactionId = null;
        }
        if (cleanTransactionId != null
                && paymentRepository.existsByTransactionId(cleanTransactionId)) {
            redirectAttributes.addFlashAttribute("error",
                    "Yeh transaction ID pehle se use ho chuki hai. Nayi ID enter karein.");
            return "redirect:/accountant/pending";
        }

        Payment payment = new Payment();
        payment.setStudentId(fee.getStudentId());
        payment.setFeeId(fee.getFeeId());
        payment.setAmount(paymentAmount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod(paymentMethod == null || paymentMethod.isBlank()
                ? "Cash" : paymentMethod.trim());
        payment.setTransactionId(cleanTransactionId);
        payment.setPaymentStatus("Paid");
        payment.setRemarks("Accountant module payment");

        BigDecimal oldPaid = fee.getPaidAmount() == null
                ? BigDecimal.ZERO : fee.getPaidAmount();
        BigDecimal newPaid = oldPaid.add(paymentAmount);
        fee.setPaidAmount(newPaid);
        fee.setPaymentDate(LocalDate.now());
        fee.setPaymentStatus(newPaid.compareTo(fee.getAmount()) >= 0
                ? "PAID" : "PARTIAL");

        paymentRepository.save(payment);
        feeRepository.save(fee);

        redirectAttributes.addFlashAttribute("message",
                "Payment record ho gaya. Updated balance: Rs. "
                + fee.getBalance());
        return "redirect:/accountant/pending";
    }

    @GetMapping("/accountant/payments/history")
    public String paymentHistory(Model model) {
        model.addAttribute("payments", paymentRepository.findAll());
        return "payment-history";
    }
    @GetMapping("/accountant/receipts")
    public String feeReceipts(Model model) {
        model.addAttribute("payments", paymentRepository.findAll());
        return "fee-receipts";
    }

    @GetMapping("/accountant/receipts/{paymentId}")
    public String feeReceipt(
            @PathVariable Long paymentId,
            Model model,
            RedirectAttributes redirectAttributes) {

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            redirectAttributes.addFlashAttribute("error", "Payment record nahi mila.");
            return "redirect:/accountant/payments/history";
        }

        Student student = studentRepository.findById(payment.getStudentId()).orElse(null);
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Student record nahi mila.");
            return "redirect:/accountant/payments/history";
        }

        Fee fee = payment.getFeeId() == null
                ? null
                : feeRepository.findById(payment.getFeeId()).orElse(null);

        model.addAttribute("payment", payment);
        model.addAttribute("student", student);
        model.addAttribute("fee", fee);

        return "fee-receipt";
    }

    @GetMapping("/accountant/reports")
    public String feeReports(Model model) {
        java.util.List<Fee> fees = feeRepository.findAll();
        java.util.List<Payment> payments = paymentRepository.findAll();

        BigDecimal totalFees = fees.stream()
            .map(Fee::getAmount)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = fees.stream()
            .map(Fee::getPaidAmount)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBalance = fees.stream()
            .map(Fee::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("fees", fees);
        model.addAttribute("payments", payments);
        model.addAttribute("totalFees", totalFees);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("totalBalance", totalBalance);

        return "fees-reports";
    }
    // Accountant Attendance: load current database records
    @GetMapping("/accountant/attendance")
    public String accountantAttendancePage(Model model) {
        model.addAttribute("students", jdbcTemplate.queryForList(
            "SELECT student_id, enrollment_no, roll_no, first_name, last_name, semester FROM students ORDER BY student_id"
        ));
        model.addAttribute("subjects", jdbcTemplate.queryForList(
            "SELECT subject_id, subject_code, subject_name, semester FROM subjects ORDER BY semester, subject_name"
        ));
        model.addAttribute("teachers", jdbcTemplate.queryForList(
            "SELECT teacher_id, employee_code, first_name, last_name FROM teachers ORDER BY first_name, last_name"
        ));
        model.addAttribute("attendanceRecords", jdbcTemplate.queryForList(
            "SELECT a.attendance_id, a.attendance_date, a.status, " +
            "s.enrollment_no, s.first_name AS student_first_name, s.last_name AS student_last_name, " +
            "sub.subject_code, sub.subject_name, t.first_name AS teacher_first_name, t.last_name AS teacher_last_name " +
            "FROM attendance a " +
            "JOIN students s ON s.student_id = a.student_id " +
            "JOIN subjects sub ON sub.subject_id = a.subject_id " +
            "LEFT JOIN teachers t ON t.teacher_id = a.teacher_id " +
            "ORDER BY a.attendance_date DESC, a.attendance_id DESC LIMIT 100"
        ));
        return "accountant/attendance-module";
    }

    @PostMapping("/accountant/attendance/save")
    public String saveAccountantAttendance(
            @RequestParam Integer studentId,
            @RequestParam Integer subjectId,
            @RequestParam String attendanceDate,
            @RequestParam String status,
            @RequestParam(required = false) Integer teacherId,
            RedirectAttributes redirectAttributes) {

        try {
            if (studentId == null || subjectId == null ||
                    attendanceDate == null || attendanceDate.isBlank() ||
                    status == null ||
                    !java.util.Set.of("PRESENT", "ABSENT", "LATE").contains(status)) {
                redirectAttributes.addFlashAttribute("error", "Student, subject, date aur valid status chunein.");
                return "redirect:/accountant/attendance";
            }

            Integer studentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE student_id = ?", Integer.class, studentId);
            Integer subjectCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subjects WHERE subject_id = ?", Integer.class, subjectId);

            if (studentCount == null || studentCount == 0 ||
                    subjectCount == null || subjectCount == 0) {
                redirectAttributes.addFlashAttribute("error", "Selected student ya subject database mein nahi mila.");
                return "redirect:/accountant/attendance";
            }

            if (teacherId != null) {
                Integer teacherCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM teachers WHERE teacher_id = ?", Integer.class, teacherId);
                if (teacherCount == null || teacherCount == 0) {
                    redirectAttributes.addFlashAttribute("error", "Selected teacher database mein nahi mila.");
                    return "redirect:/accountant/attendance";
                }
            }

            LocalDate date = LocalDate.parse(attendanceDate);
            Integer duplicateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND subject_id = ? AND attendance_date = ?",
                Integer.class, studentId, subjectId, date);

            if (duplicateCount != null && duplicateCount > 0) {
                redirectAttributes.addFlashAttribute("error",
                    "Is student aur subject ki attendance is date par pehle se maujood hai.");
                return "redirect:/accountant/attendance";
            }

            if (teacherId == null) {
                jdbcTemplate.update(
                    "INSERT INTO attendance (student_id, subject_id, teacher_id, attendance_date, status) VALUES (?, ?, NULL, ?, ?)",
                    studentId, subjectId, date, status);
            } else {
                jdbcTemplate.update(
                    "INSERT INTO attendance (student_id, subject_id, teacher_id, attendance_date, status) VALUES (?, ?, ?, ?, ?)",
                    studentId, subjectId, teacherId, date, status);
            }

            redirectAttributes.addFlashAttribute("message", "Attendance database mein save ho gayi.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                "Attendance save nahi hui. Date aur database constraints check karein.");
        }

        return "redirect:/accountant/attendance";
    }

    // Examination Module Integration
    @GetMapping({
        "/admin/examination-module.html",
        "/admin/examinations",
        "/accountant/examination-module.html",
        "/accountant/examinations"
    })
    public String examinationModule(Model model) {
        model.addAttribute("courses", jdbcTemplate.queryForList(
            "SELECT course_id, course_name, course_code FROM courses ORDER BY course_name"
        ));

        model.addAttribute("exams", jdbcTemplate.queryForList(
            "SELECT e.exam_id, e.exam_name, e.exam_type, e.semester, " +
            "e.exam_date, e.start_time, e.end_time, e.academic_year, " +
            "COALESCE(e.status, 'Scheduled') AS status, c.course_name " +
            "FROM exams e LEFT JOIN courses c ON e.course_id = c.course_id " +
            "ORDER BY e.exam_date DESC, e.exam_id DESC"
        ));

        return "examination-module";
    }

    // RESULT GENERATION - only marks linked to the selected examination
    @GetMapping({"/admin/result-generation", "/admin/result-generation.html"})
    public String resultGeneration(
            @RequestParam(required = false) Long examId,
            Model model) {

        model.addAttribute("exams", jdbcTemplate.queryForList(
            "SELECT exam_id, exam_name, exam_type, exam_date " +
            "FROM exams ORDER BY exam_date DESC, exam_id DESC"
        ));

        model.addAttribute("selectedExamId", examId);

        Integer linkedMarks = 0;
        if (examId != null) {
            linkedMarks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM marks WHERE exam_id = ?",
                Integer.class, examId
            );
        }

        model.addAttribute("linkedMarks", linkedMarks == null ? 0 : linkedMarks);

        String resultSql =
            "SELECT er.result_id, e.exam_name, s.enrollment_no, " +
            "CONCAT(s.first_name, ' ', COALESCE(s.last_name, '')) AS student_name, " +
            "sub.subject_code, sub.subject_name, er.marks_obtained, " +
            "er.total_marks, er.percentage, er.grade, er.result_status " +
            "FROM exam_results er " +
            "JOIN exams e ON e.exam_id = er.exam_id " +
            "JOIN students s ON s.student_id = er.student_id " +
            "LEFT JOIN subjects sub ON sub.subject_id = er.subject_id ";

        if (examId != null) {
            model.addAttribute("results", jdbcTemplate.queryForList(
                resultSql + "WHERE er.exam_id = ? ORDER BY s.enrollment_no, sub.subject_name",
                examId
            ));
        } else {
            model.addAttribute("results", jdbcTemplate.queryForList(
                resultSql + "ORDER BY e.exam_date DESC, s.enrollment_no, sub.subject_name"
            ));
        }

        return "result-generation";
    }

    // Generate results without updating or deleting existing result rows
    @PostMapping("/admin/result-generation/generate")
    @Transactional
    public String generateResults(
            @RequestParam Long examId,
            RedirectAttributes redirectAttributes) {

        try {
            Integer examCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exams WHERE exam_id = ?",
                Integer.class, examId
            );

            if (examCount == null || examCount == 0) {
                redirectAttributes.addFlashAttribute("error",
                    "Selected examination record nahi mila.");
                return "redirect:/admin/result-generation";
            }

            Integer marksCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM marks WHERE exam_id = ?",
                Integer.class, examId
            );

            if (marksCount == null || marksCount == 0) {
                redirectAttributes.addFlashAttribute("error",
                    "Is examination se linked marks nahi mile. Pehle Teacher Marks module mein isi Exam ko select karke marks save karein. Purane unlinked marks use nahi honge.");
                return "redirect:/admin/result-generation?examId=" + examId;
            }

            int inserted = jdbcTemplate.update(
                "INSERT INTO exam_results " +
                "(exam_id, student_id, subject_id, marks_obtained, total_marks, percentage, grade, result_status) " +
                "SELECT m.exam_id, m.student_id, m.subject_id, " +
                "SUM(m.marks_obtained), SUM(m.max_marks), " +
                "ROUND(SUM(m.marks_obtained) * 100.0 / NULLIF(SUM(m.max_marks), 0), 2), " +
                "CASE " +
                "WHEN SUM(m.marks_obtained) * 100.0 / NULLIF(SUM(m.max_marks), 0) >= 90 THEN 'A+' " +
                "WHEN SUM(m.marks_obtained) * 100.0 / NULLIF(SUM(m.max_marks), 0) >= 80 THEN 'A' " +
                "WHEN SUM(m.marks_obtained) * 100.0 / NULLIF(SUM(m.max_marks), 0) >= 70 THEN 'B' " +
                "WHEN SUM(m.marks_obtained) * 100.0 / NULLIF(SUM(m.max_marks), 0) >= 60 THEN 'C' " +
                "WHEN SUM(m.marks_obtained) * 100.0 / NULLIF(SUM(m.max_marks), 0) >= 40 THEN 'D' " +
                "ELSE 'F' END, " +
                "CASE WHEN SUM(m.marks_obtained) * 100.0 / NULLIF(SUM(m.max_marks), 0) >= 40 " +
                "THEN 'PASS' ELSE 'FAIL' END " +
                "FROM marks m " +
                "WHERE m.exam_id = ? AND m.max_marks > 0 " +
                "GROUP BY m.exam_id, m.student_id, m.subject_id " +
                "HAVING SUM(m.max_marks) > 0 " +
                "AND NOT EXISTS (" +
                "SELECT 1 FROM exam_results er " +
                "WHERE er.exam_id = m.exam_id " +
                "AND er.student_id = m.student_id " +
                "AND er.subject_id = m.subject_id)",
                examId
            );

            redirectAttributes.addFlashAttribute("message",
                "Result Generation complete. New subject results: " + inserted +
                ". Existing results were not overwritten.");

        } catch (org.springframework.dao.DataAccessException ex) {
            redirectAttributes.addFlashAttribute("error",
                "Database error: Result generate nahi hua. Existing data ko update ya delete nahi kiya gaya.");
        }

        return "redirect:/admin/result-generation?examId=" + examId;
    }

    // STUDENT-WISE RESULT REPORT
    @GetMapping({"/admin/student-result-report", "/admin/student-result-report.html"})
    public String studentResultReport(
            @RequestParam(required = false) Integer studentId,
            Model model) {

        model.addAttribute("students", jdbcTemplate.queryForList(
            "SELECT student_id, enrollment_no, first_name, last_name " +
            "FROM students ORDER BY first_name, last_name"
        ));

        model.addAttribute("selectedStudentId", studentId);

        if (studentId != null) {
            model.addAttribute("studentResults", jdbcTemplate.queryForList(
                "SELECT e.exam_name, e.exam_type, e.exam_date, " +
                "sub.subject_code, sub.subject_name, er.marks_obtained, " +
                "er.total_marks, er.percentage, er.grade, er.result_status " +
                "FROM exam_results er " +
                "JOIN exams e ON e.exam_id = er.exam_id " +
                "LEFT JOIN subjects sub ON sub.subject_id = er.subject_id " +
                "WHERE er.student_id = ? " +
                "ORDER BY e.exam_date DESC, sub.subject_name",
                studentId
            ));

            model.addAttribute("studentSummary", jdbcTemplate.queryForList(
                "SELECT e.exam_name, SUM(er.marks_obtained) AS obtained_marks, " +
                "SUM(er.total_marks) AS total_marks, " +
                "ROUND(SUM(er.marks_obtained) * 100.0 / NULLIF(SUM(er.total_marks), 0), 2) AS percentage, " +
                "CASE WHEN SUM(er.marks_obtained) * 100.0 / NULLIF(SUM(er.total_marks), 0) >= 40 " +
                "THEN 'PASS' ELSE 'FAIL' END AS result_status " +
                "FROM exam_results er JOIN exams e ON e.exam_id = er.exam_id " +
                "WHERE er.student_id = ? GROUP BY e.exam_id, e.exam_name, e.exam_date " +
                "ORDER BY e.exam_date DESC",
                studentId
            ));
        } else {
            model.addAttribute("studentResults", java.util.Collections.emptyList());
            model.addAttribute("studentSummary", java.util.Collections.emptyList());
        }

        return "student-result-report";
    }
    @PostMapping("/admin/examinations/save")
    public String saveExamination(
            @RequestParam String examName,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String examDate,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {

        try {
            if (examName == null || examName.isBlank()) {
                throw new IllegalArgumentException("Exam name is required.");
            }

            jdbcTemplate.update(
                "INSERT INTO exams " +
                "(exam_name, exam_type, course_id, semester, exam_date, " +
                "start_time, end_time, academic_year, status) " +
                "VALUES (?, NULLIF(?, ''), NULLIF(?, ''), NULLIF(?, ''), " +
                "STR_TO_DATE(NULLIF(?, ''), '%Y-%m-%d'), " +
                "STR_TO_DATE(NULLIF(?, ''), '%H:%i'), " +
                "STR_TO_DATE(NULLIF(?, ''), '%H:%i'), NULLIF(?, ''), ?)",
                examName.trim(),
                examType == null ? "" : examType.trim(),
                courseId == null ? "" : courseId.trim(),
                semester == null ? "" : semester.trim(),
                examDate == null ? "" : examDate.trim(),
                startTime == null ? "" : startTime.trim(),
                endTime == null ? "" : endTime.trim(),
                academicYear == null ? "" : academicYear.trim(),
                status == null || status.isBlank() ? "Scheduled" : status.trim()
            );

            redirectAttributes.addFlashAttribute(
                "message", "Examination database mein save ho gaya."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                "error",
                "Exam save nahi hua. Exam name, course/semester aur database constraints check karein."
            );
        }

        return "redirect:/admin/examination-module.html";
    }


    // Edit Examination - load existing details
    @GetMapping("/admin/examinations/{examId}/edit")
    public String editExamination(
            @PathVariable Long examId,
            Model model,
            RedirectAttributes redirectAttributes) {

        java.util.List<java.util.Map<String, Object>> rows =
            jdbcTemplate.queryForList(
                "SELECT exam_id, exam_name, exam_type, course_id, semester, " +
                "DATE_FORMAT(exam_date, '%Y-%m-%d') AS exam_date_input, " +
                "TIME_FORMAT(start_time, '%H:%i') AS start_time_input, " +
                "TIME_FORMAT(end_time, '%H:%i') AS end_time_input, " +
                "academic_year, COALESCE(status, 'Scheduled') AS status " +
                "FROM exams WHERE exam_id = ?",
                examId
            );

        if (rows.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Examination record nahi mila.");
            return "redirect:/admin/examination-module.html";
        }

        model.addAttribute("exam", rows.get(0));
        model.addAttribute("courses", jdbcTemplate.queryForList(
            "SELECT course_id, course_name, course_code FROM courses ORDER BY course_name"
        ));

        return "examination-edit";
    }

    // Update Examination
    @PostMapping("/admin/examinations/{examId}/update")
    public String updateExamination(
            @PathVariable Long examId,
            @RequestParam String examName,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String examDate,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {

        try {
            if (examName == null || examName.isBlank()) {
                throw new IllegalArgumentException("Exam name is required.");
            }

            int updated = jdbcTemplate.update(
                "UPDATE exams SET exam_name = ?, exam_type = NULLIF(?, ''), " +
                "course_id = NULLIF(?, ''), semester = NULLIF(?, ''), " +
                "exam_date = STR_TO_DATE(NULLIF(?, ''), '%Y-%m-%d'), " +
                "start_time = STR_TO_DATE(NULLIF(?, ''), '%H:%i'), " +
                "end_time = STR_TO_DATE(NULLIF(?, ''), '%H:%i'), " +
                "academic_year = NULLIF(?, ''), status = ? WHERE exam_id = ?",
                examName.trim(),
                examType == null ? "" : examType.trim(),
                courseId == null ? "" : courseId.trim(),
                semester == null ? "" : semester.trim(),
                examDate == null ? "" : examDate.trim(),
                startTime == null ? "" : startTime.trim(),
                endTime == null ? "" : endTime.trim(),
                academicYear == null ? "" : academicYear.trim(),
                status == null || status.isBlank() ? "Scheduled" : status.trim(),
                examId
            );

            if (updated > 0) {
                redirectAttributes.addFlashAttribute("message", "Examination successfully update ho gaya.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Examination nahi mila ya koi change nahi hua.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Examination update nahi hua. Details aur database constraints check karein.");
        }

        return "redirect:/admin/examination-module.html";
    }

    // Delete Examination - protect exams that already have results
    @PostMapping("/admin/examinations/{examId}/delete")
    public String deleteExamination(
            @PathVariable Long examId,
            RedirectAttributes redirectAttributes) {

        try {
            Integer resultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_results WHERE exam_id = ?",
                Integer.class,
                examId
            );

            if (resultCount != null && resultCount > 0) {
                redirectAttributes.addFlashAttribute(
                    "error",
                    "Is examination ke results maujood hain. Data safety ke liye delete block kar diya gaya."
                );
            } else {
                int deleted = jdbcTemplate.update(
                    "DELETE FROM exams WHERE exam_id = ?",
                    examId
                );

                if (deleted > 0) {
                    redirectAttributes.addFlashAttribute("message", "Examination delete ho gaya.");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Examination record nahi mila.");
                }
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                "error",
                "Examination delete nahi hua. Database constraints check karein."
            );
        }

        return "redirect:/admin/examination-module.html";
    }

    // Compatibility redirect for the old Teacher Dashboard URL
    @GetMapping("/teacher-dashboard.html")
    public String legacyTeacherDashboard() {
        return "redirect:/teacher/dashboard";
    }

    @org.springframework.web.bind.annotation.GetMapping("/admin/subjects")
    public String adminSubjects(org.springframework.ui.Model model) {
        model.addAttribute("subjects", jdbcTemplate.queryForList(
            "SELECT subject_id, subject_code, subject_name, semester, credits " +
            "FROM subjects ORDER BY subject_name"
        ));
        return "subjects";
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/subjects/save")
    public String saveAdminSubject(
            @org.springframework.web.bind.annotation.RequestParam("subjectCode") String subjectCode,
            @org.springframework.web.bind.annotation.RequestParam("subjectName") String subjectName,
            @org.springframework.web.bind.annotation.RequestParam("semester") Integer semester,
            @org.springframework.web.bind.annotation.RequestParam(value = "credits", required = false) Integer credits,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        subjectCode = subjectCode == null ? "" : subjectCode.trim();
        subjectName = subjectName == null ? "" : subjectName.trim();

        if (subjectCode.isEmpty() || subjectCode.length() > 30) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Subject code required hai aur 30 characters se zyada nahi ho sakta.");
            return "redirect:/admin/subjects";
        }

        if (subjectName.isEmpty() || subjectName.length() > 100) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Subject name required hai aur 100 characters se zyada nahi ho sakta.");
            return "redirect:/admin/subjects";
        }

        if (semester == null || semester < 1) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Semester 1 ya usse bada number hona chahiye.");
            return "redirect:/admin/subjects";
        }

        if (credits == null) {
            credits = 0;
        }
        if (credits < 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Credits negative nahi ho sakte.");
            return "redirect:/admin/subjects";
        }

        try {
            Integer duplicate = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subjects WHERE subject_code = ?",
                Integer.class, subjectCode
            );

            if (duplicate != null && duplicate > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "Yeh subject code pehle se maujood hai.");
                return "redirect:/admin/subjects";
            }

            jdbcTemplate.update(
                "INSERT INTO subjects (subject_code, subject_name, semester, credits) VALUES (?, ?, ?, ?)",
                subjectCode, subjectName, semester, credits
            );

            redirectAttributes.addFlashAttribute("successMessage",
                "Subject successfully save ho gaya.");
        } catch (org.springframework.dao.DataAccessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Database mein subject save nahi hua. Subject code aur database constraints check karein.");
        }

        return "redirect:/admin/subjects";
    }}
