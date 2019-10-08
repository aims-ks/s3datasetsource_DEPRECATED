package thredds.server.harvester;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generate a single catalog.xml file using FreeMarker
 */
public class CatalogTemplate {
    private Template template;

    private List<Dataset> datasets;
    private List<CatalogRef> catalogRefs;

    public CatalogTemplate() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);

        // This is used to do something similar to:
        //   CatalogTemplate.class.getClassLoader().getResource("/templates");
        cfg.setClassForTemplateLoading(CatalogTemplate.class, "/templates");

        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.UK);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        this.template = cfg.getTemplate("catalog.ftl");
        this.datasets = new ArrayList<Dataset>();
        this.catalogRefs = new ArrayList<CatalogRef>();
    }

    public void addDataset(String name, String id, String urlPath) {
        this.datasets.add(new Dataset(name, id, urlPath));
    }

    public void addCatalogRef(String name, String href) {
        this.catalogRefs.add(new CatalogRef(name, href));
    }

    public void process(File catalogFile) throws IOException, TemplateException {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("datasets", this.datasets);
        input.put("catalogRefs", this.catalogRefs);

        // Debug - write to the console
        //Writer consoleWriter = new OutputStreamWriter(System.out);
        //this.template.process(input, consoleWriter);

        File catalogDir = catalogFile.getParentFile();
        catalogDir.mkdirs();

        // Save into the file
        try (Writer fileWriter = new FileWriter(catalogFile)) {
            this.template.process(input, fileWriter);
        }
    }

    public static class Dataset {
        private String name;
        private String id;
        private String urlPath;

        public Dataset(String name, String id, String urlPath) {
            this.name = name;
            this.id = id;
            this.urlPath = urlPath;
        }

        public String getName() {
            return this.name;
        }

        public String getId() {
            return this.id;
        }

        public String getUrlPath() {
            return this.urlPath;
        }
    }

    public static class CatalogRef {
        private String name;
        private String href;

        public CatalogRef(String name, String href) {
            this.name = name;
            this.href = href;
        }

        public String getName() {
            return this.name;
        }

        public String getHref() {
            return this.href;
        }
    }
}
