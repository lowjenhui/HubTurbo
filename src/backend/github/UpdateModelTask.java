package backend.github;

import backend.Model;
import backend.UpdateSignature;
import backend.interfaces.Repo;
import backend.interfaces.RepoTask;
import org.eclipse.egit.github.core.Issue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

class UpdateModelTask extends GitHubRepoTask<Model> {

	private final Model model;

	public UpdateModelTask(BlockingQueue<RepoTask<?, ?>> tasks, Repo<Issue> repo, Model model) {
		super(tasks, repo);
		this.model = model;
	}

	@Override
	public void run() {
		UpdateIssuesTask issuesTask = new UpdateIssuesTask(tasks, repo, model);
		tasks.add(issuesTask);

		try {
			UpdateIssuesTask.Result issuesResult = issuesTask.response.get();
			// TODO fill out the others, don't leave them as null
			UpdateSignature newSignature =
				new UpdateSignature(issuesResult.ETag, null, null, null, issuesResult.lastCheckTime);
			Model result = new Model(model.getRepoId(), newSignature).withIssues(issuesResult.issues);
			response.complete(result);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
