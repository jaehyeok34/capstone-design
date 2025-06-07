BRANCHES = api-gateway \
	data-server \
	pii-detection-server \
	matching-key-server \
	matching-server \
	pseudonymization-server 

a:
	@for branch in $(BRANCHES); do \
		git worktree add -b $$branch -f $$branch-worktree origin/$$branch; \
	done


rf:
	@for branch in $(BRANCHES); do \
		git worktree remove --force $$branch-worktree; \
		git branch -d $$branch; \
	done

u:
	docker-compose up -d --build

d:
	docker-compose down

du:
	make d
	make u