package pe.edu.upc;

import java.io.IOException;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.upc.encrypt.PropertyServiceForJasyptStarter;
import pe.edu.upc.model.Admin;
import pe.edu.upc.model.Certification;

@Controller
@RequestMapping("/")
public class CertificationController {

	@Autowired
	CertificationService certificationService;

	@Autowired
	PropertyServiceForJasyptStarter encryptorService;

	private Certification certification;
	private boolean loginFlag;

	@PostConstruct
	void init() {
		setLoginFlag(false);
	}

	@RequestMapping("/home")
	public String goGenerate(Model model) {
		if (loginFlag == false) {
			return "redirect:/";
		}
		setCertification(new Certification());
		model.addAttribute("certification", certification);
		setLoginFlag(false);
		return "certification";
	}

	@RequestMapping("/goHome")
	public String goHome(@ModelAttribute Admin admin, BindingResult binRes, Model model,
			RedirectAttributes redirectAttributes) {
		if (encryptorService.getAdminPassword().equals(admin.getPassword())) {
			setLoginFlag(true);
			return "redirect:/home";
		} else if (admin.getPassword().isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Contraseña en blanco..");
			return "redirect:/";
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Contraseña incorrecta.");
			return "redirect:/";
		}
	}

	@RequestMapping("/")
	public String login(Model model) {
		model.addAttribute("admin", new Admin());
		return "login";
	}

	@RequestMapping("/generateCertification")
	public void generate(@ModelAttribute Certification certification, BindingResult binRes, Model model,
			RedirectAttributes redirectAttributes, HttpServletResponse response) throws Exception {
		setCertification(certification);
		if (certification == null)
			certification = new Certification();
		if (binRes.hasErrors()) {
			System.out.println("Error en BindingResult");
		} else {

			boolean flag = true;
			if ((getCertification().getStudent1().isEmpty() || getCertification().getStudent1() == null)
					&& (getCertification().getStudent2().isEmpty() || getCertification().getStudent2() == null)) {
				redirectAttributes.addFlashAttribute("errorMessage", "Debe colocar al menos un estudiante.");
				flag = false;
			}
			if (getCertification().getTitle().isEmpty() || getCertification().getTitle() == null) {
				redirectAttributes.addFlashAttribute("errorMessage", "Debe colocar el nombre del proyecto.");
				flag = false;
			}

			String jaspertRoute = Paths.get("").toFile().getAbsolutePath() + "/src/main/webapp/report";
			System.out.println(flag);
			if (flag == true) {
				byte[] bytes = certificationService.generateCertification(jaspertRoute, certification);
				setPDFResponse(response, bytes, getFileName());
			} else return;
		}
	}

	public String getFileName() {
		String part1[] = { "", "" };
		String part2[] = { "", "" };
		if (!getCertification().getStudent1().isEmpty()) {
			part1 = getCertification().getStudent1().split(" ", 2);
			part1[0] = "_" + part1[0];
		}
		if (!getCertification().getStudent2().isEmpty()) {
			part2 = getCertification().getStudent2().split(" ", 2);
			part2[0] = "_" + part2[0];
		}
		return "Certificado" + part1[0] + part2[0];
	}

	public static void setPDFResponse(HttpServletResponse response, byte[] bytes, String filename) throws IOException {
		response.reset();
		response.setContentType("application/octet-stream");
		response.setContentLength(bytes.length);
		response.setHeader("Content-disposition", "attachment; filename=\"" + filename + ".pdf\"");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		ServletOutputStream ouputStream = response.getOutputStream();
		ouputStream.write(bytes, 0, bytes.length);
		ouputStream.flush();
		ouputStream.close();
	}

	public Certification getCertification() {
		return certification;
	}

	public void setCertification(Certification certification) {
		this.certification = certification;
	}

	public boolean isLoginFlag() {
		return loginFlag;
	}

	public void setLoginFlag(boolean loginFlag) {
		this.loginFlag = loginFlag;
	}
}
