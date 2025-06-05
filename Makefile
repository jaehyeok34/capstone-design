add:
	git worktree add -f api-gateway/ origin/api-gateway
	git worktree add -f matching-key-server/ origin/matching-key-server
	git worktree add -f data-server/ origin/data-server
	git worktree add -f matching-server/ origin/matching-server  
	git worktree add -f pseudonymization-server/ origin/pseudonymization-server

rm:
	git worktree remove $(CURDIR)/api-gateway
	git worktree remove $(CURDIR)/matching-key-server
	git worktree remove $(CURDIR)/data-server
	git worktree remove $(CURDIR)/matching-server
	git worktree remove $(CURDIR)/pseudonymization-server
	
rf:
	git worktree remove --force $(CURDIR)/api-gateway
	git worktree remove --force $(CURDIR)/matching-key-server
	git worktree remove --force $(CURDIR)/data-server
	git worktree remove --force $(CURDIR)/matching-server
	git worktree remove --force $(CURDIR)/pseudonymization-server