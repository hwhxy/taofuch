<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<jsp:include page="../include/header.jsp" />
<center>
	<table>
		<tr>
			<th> </th>
			<c:forEach var="title" items="${systems}">
				<th style="text-align: center;">${title.name}</th>
			</c:forEach>
		</tr>
		<c:forEach var="row" items="${systems}">
			<tr>
				<th>${row.name}</th>
				<c:forEach var="column" items="${systems}">
					<td align="center">
						<c:if test="${row.hasRelation(row.sysId, column.sysId)}">
							<input type="checkbox" name="relations" value="${row.sysId}_${column.sysId}"
							 disabled="disabled" checked/>${row.getRelation(row.sysId, column.sysId).introduce}
						</c:if>
					</td>
				</c:forEach>
			</tr>
		</c:forEach>
	</table>
	<input type="button" value="关系配置" onclick="window.location.href='/relationship/conf'"/>
</center>
