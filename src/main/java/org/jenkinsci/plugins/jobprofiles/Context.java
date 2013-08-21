package org.jenkinsci.plugins.jobprofiles;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenUtil;
import hudson.model.TaskListener;
import hudson.tasks.Maven;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Context {
    public static Map<String, Object> get(SoftwareAsset asset, Scm scm, World world) throws IOException {
        return getMavenContext(world, scm, asset);
    }

    public static Map<String, Object> getMavenContext(World world, Scm scm, SoftwareAsset asset) throws IOException {
        Map<String, Object> context;
        MavenProject project;
        String pom;

        context = new HashMap<String, Object>();
        pom = scm.getPom();

        if (pom == null) return context;
        project = getMavenProject(pom, world);

        asset.setGroupId(project.getGroupId());
        asset.setArtifactId(project.getArtifactId());
        asset.setCategory(asset.getCategory() == null ? "No Category" : asset.getCategory());
        asset.setId(asset.getId() == null ? "Freestyle" : asset.getId());
        context.put("mavenproject", project);
        context.put("name", project.getArtifactId());
        context.put("asset", asset);
        for (Map.Entry entry : project.getProperties().entrySet()) {
            context.put(entry.getKey().toString().replace(".", "_"), entry.getValue());
        }

        return context;
    }


    public static MavenProject getMavenProject(String pomContent, World world) {
        MavenEmbedder embedder;
        MavenProject mavenProject;
        FileNode tmpPom;
        File mavenHome;
        Maven.MavenInstallation[] installations;
        TaskListener listener;

        installations = Jenkins.getInstance().getDescriptorByType(hudson.tasks.Maven.DescriptorImpl.class).getInstallations();

        if (installations.length == 0) {
            throw new JobProfileException("No Maven installation found.");
        }

        mavenHome = installations[0].getHomeDir();
        listener = new LogTaskListener(Logger.getLogger(Context.class.toString()), Level.ALL);

        try {
            tmpPom = (FileNode) world.getTemp().createTempFile().writeStrings(pomContent);
        } catch (IOException e) {
            throw new JobProfileException(e.getMessage(), e.getCause());
        }

        try {
            assert tmpPom != null;
            embedder = MavenUtil.createEmbedder(listener, mavenHome, null);
            mavenProject = embedder.readProject(new File(tmpPom.toString()));
        } catch (MavenEmbedderException e) {
            throw new JobProfileException(e.getMessage(), e);
        } catch (IOException e) {
            throw new JobProfileException(e.getMessage(), e);
        } catch (ProjectBuildingException e) {
            throw new JobProfileException(e.getMessage(), e);
        }
        assert mavenProject != null;
        return mavenProject;
    }

}
