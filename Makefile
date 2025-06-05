add:
	git worktree add -f api-gateway/ origin/api-gateway
	git worktree add -f matching-key-server/ origin/matching-key-server
	git worktree add -f data-server/ origin/data-server

rm:
	git worktree remove $(CURDIR)/api-gateway
	git worktree remove $(CURDIR)/matching-key-server
	
rf:
	git worktree remove --force $(CURDIR)/api-gateway
	git worktree remove --force $(CURDIR)/matching-key-server